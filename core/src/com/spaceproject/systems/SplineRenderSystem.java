package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.SplineComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

import java.util.Comparator;

public class SplineRenderSystem extends SortedIteratingSystem implements Disposable {
    
    private static class ZComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity entityA, Entity entityB) {
            return (int)Math.signum(Mappers.spline.get(entityA).zOrder - Mappers.spline.get(entityB).zOrder);
        }
    }
    
    private final ShapeRenderer shape;
    private final Color tmpColor = new Color();
    private int maxPathSize = 1000;
    private float alpha = 1;
    private float animation;
    private float animSpeed = 2f;
    
    public SplineRenderSystem() {
        super(Family.all(SplineComponent.class, TransformComponent.class).get(), new ZComparator());
        shape = new ShapeRenderer();
    }
    
    @Override
    public void update(float delta) {
        //warning: system coupling -> todo: use signals?
        HUDSystem hudSystem = getEngine().getSystem(HUDSystem.class);
        if (hudSystem != null && !hudSystem.isDraw()) {
            return;
        }
    
        alpha = MathUtils.clamp((GameScreen.cam.zoom / 100), 0, 1);
        if (MathUtils.isEqual(alpha, 0)) return;
        
        animation += animSpeed * delta;
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shape.setProjectionMatrix(GameScreen.cam.combined);
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        super.update(delta);
        shape.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        SplineComponent spline = Mappers.spline.get(entity);
        
        updateTail(spline, entity);
        //todo: z-index to render player trail above bullet trail
        //todo: fade nicely
        if (spline.style == null) {
            spline.style = SplineComponent.Style.velocity;
        }
        switch (spline.style) {
            case velocity: renderVelocityPath(spline); break;
            case state: renderStatePath(spline); break;
            default:
                renderDefaultPath(spline);
        }
    }
    
    // these are more like paths than splines, but see also:
    // https://libgdx.com/wiki/math-utils/path-interface-and-splines
    private void updateTail(SplineComponent spline, Entity entity) {
        TransformComponent transform = Mappers.transform.get(entity);
        PhysicsComponent physics = Mappers.physics.get(entity);
        
        //initialize
        if (spline.path == null) {
            //todo: cache. use pooled vectors
            spline.path = new Vector3[maxPathSize];
            for (int v = 0; v < maxPathSize; v++) {
                spline.path[v] = new Vector3();
            }
            spline.state = new byte[maxPathSize];
        }
        
        //
        //todo: don't record useless points (duplicate). consider resolution
        //if (spline.path[spline.index-1].epsilonEquals(transform.pos.x, transform.pos.y, 10)) {
            //delta too small skip update
        //    return;
        //}
        
        //off, on, boost, hyper -> 0, 1, 2, 3
        float velocity = 0;
        byte state = 0;
        
        if (physics != null) {
            //set velocity
            velocity = physics.body.getLinearVelocity().len2();
            
            if (spline.style == SplineComponent.Style.state) {
                //set state
                ControllableComponent control = Mappers.controllable.get(entity);
                if (control != null) {
                    if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight || control.boost) {
                        state = 1;
                        if (control.boost) {
                            state = 2;
                        }
                    }
                }
                
                ShieldComponent shield = Mappers.shield.get(entity);
                if (shield != null) {
                    if (shield.state == ShieldComponent.State.on) {
                        state = -2;
                    } else {
                        //charge discharge
                        //in out
                        //state =- 4;?
                    }
                    
                    if ((GameScreen.getGameTimeCurrent() - shield.lastHit) < 500) {
                        state = -3;
                    }
                    
                }
                
                if (!physics.body.isActive()) {
                    state = 3;
    
                    HyperDriveComponent hyper = Mappers.hyper.get(entity);
                    velocity = hyper.speed * hyper.speed;
                }
                
                //todo: state -1:
                // health compontent : lastHitTime -simple timer or timestamp relative to current time
            }
        }
        
        //show hurt
        HealthComponent health = Mappers.health.get(entity);
        if (health != null) {
            long hurtTime = 1000;
            if ((GameScreen.getGameTimeCurrent() - health.lastHit) < hurtTime) {
                state = -1;
            }
        }
        
        spline.path[spline.indexHead].set(transform.pos.x, transform.pos.y, velocity);
        if (spline.style == SplineComponent.Style.state) {
            spline.state[spline.indexHead] = state;
        }
        
        //roll index
        spline.stepCount++;
        spline.indexHead++;
        if (spline.indexHead >= spline.path.length) {
            spline.indexHead = 0;
        }
    }
    
    public void renderVelocityPath(SplineComponent spline) {
        //todo: bug with tail index not properly rendered
        boolean debugDrawHeadTail = false;
    
        //set default color
        if (spline.color == null) {
            spline.color = Color.MAGENTA;
        }
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap <= 0) indexWrap += spline.path.length;
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.indexHead) {
                //z = linearVelocity [0 - max box2d] then hyperdrive velocity
                float velocity = p.z / Box2DPhysicsSystem.getVelocityLimit2();
                tmpColor.set(Color.BLACK.cpy().lerp(spline.color, velocity));
                tmpColor.a = alpha;
                if (velocity > 1.01f) {
                    //hyperdrive travel
                    tmpColor.set(1, 1, 1, 1);
                }
                shape.line(p.x, p.y, p2.x, p2.y, tmpColor, tmpColor);
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
        }
    }
    
    
    public void renderStatePath(SplineComponent spline) {
        //todo: bug with tail index not properly rendered
        boolean debugDrawHeadTail = false;
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap <= 0) {
                indexWrap += spline.path.length;
            }
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.indexHead) {
                Color color;
                switch (spline.state[indexWrap]) {
                    case -3: color = Color.GREEN; break; //or color of asteroid hit?
                    case -2: color = Color.BLUE; break;
                    case -1: color = Color.RED; break;
                    case 1: color = Color.GOLD; break;
                    case 2: color = Color.CYAN; break;
                    case 3: color = Color.WHITE; break;
                    default: color = Color.BLACK;
                }
                shape.line(p.x, p.y, p2.x, p2.y, color, color);
                if (spline.state[indexWrap] == 3) {
                    shape.rectLine(p.x, p.y, p2.x, p2.y, (float) (0.5f * Math.sin(animation * 3.14 * 0.5)), Color.RED, Color.RED);
                    shape.rectLine(p.x, p.y, p2.x, p2.y, (float) (0.5f * Math.sin(animation)), Color.MAGENTA, Color.CYAN);
                    shape.rectLine(p.x, p.y, p2.x, p2.y, (float) (0.5f * Math.sin(animation * 3.14)), Color.GOLD, Color.GREEN);
                }
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
        }
    }
    
    public void renderDefaultPath(SplineComponent spline) {
        //todo: bug with tail index not properly rendered
        boolean debugDrawHeadTail = false;
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap <= 0) indexWrap += spline.path.length;
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.indexHead) {
                if (spline.color == null) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.GREEN, Color.BLUE);
                } else {
                    shape.line(p.x, p.y, p2.x, p2.y, spline.color, spline.color);
                }
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
    
            if (spline.indexHead == 0) {
                shape.line(p.x, p.y, p2.x, p2.y, Color.PINK, Color.PURPLE);
            }
        }
    }
    
    public void renderRainbowPath(SplineComponent spline) {
        //todo: rainbow render
        //xy mode
        //change of angle: same color when going straight, 360 otherwise, doesnt care speed
        //change of speed: 0-max, doesn't care angle
    }
    
    private Color backgroundColor(OrthographicCamera cam) {
        //still playing with these values to get the right feel/intensity of color...
        float camZoomBlackScale = 500.0f;
        float maxColor = 0.25f;
        float ratio = 0.0001f;
        float green = Math.abs(cam.position.x * ratio);
        float blue = Math.abs(cam.position.y * ratio);
        //green based on x position. range amount of green between 0 and maxColor
        if ((int) (green / maxColor) % 2 == 0) {
            green %= maxColor;
        } else {
            green = maxColor - green % maxColor;
        }
        //blue based on y position. range amount of blue between 0 and maxColor
        if ((int) (blue / maxColor) % 2 == 0) {
            blue %= maxColor;
        } else {
            blue = maxColor - blue % maxColor;
        }
        float red = blue + green;
        tmpColor.set(red, green + (maxColor - red) + 0.2f, blue + (maxColor - red) + 0.1f, 1);
        
        tmpColor.lerp(Color.BLACK, MathUtils.clamp(cam.zoom / camZoomBlackScale, 0, 1)); //fade to black on zoom out
        return tmpColor;
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }
    
}
