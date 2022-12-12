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
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.PhysicsComponent;
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
    
    public SplineRenderSystem() {
        super(Family.all(SplineComponent.class, TransformComponent.class).get(), new ZComparator());
        shape = new ShapeRenderer();
    }
    
    @Override
    public void update(float delta) {
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
            spline.style = SplineComponent.Style.solid;
        }
        switch (spline.style) {
            case velocity: renderVelocityPath(spline); break;
            case rainbow: renderRainbowPath(spline); break;
            default:
                renderPath(spline);
        }
    }
    
    //see also: https://libgdx.com/wiki/math-utils/path-interface-and-splines
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
        }
        
        //
        //todo: don't record useless points (duplicate). consider resolution
        //if (spline.path[spline.index].epsilonEquals(transform.pos.x, transform.pos.y, 10)) {
            //delta too small skip update
        //    return;
        //}
    
        float velocity = 0;//todo: diff between now and previous point.
        if (physics != null) {
            velocity = physics.body.getLinearVelocity().len2();
            if (!physics.body.isActive()) {
                HyperDriveComponent hyper = Mappers.hyper.get(entity);
                velocity = hyper.speed * hyper.speed;
            }
        }
        spline.path[spline.index].set(transform.pos.x, transform.pos.y, velocity);
        //roll index
        spline.index++;
        if (spline.index >= spline.path.length) {
            spline.index = 0;
        }
    }
    
    public void renderVelocityPath(SplineComponent spline) {
        //todo: bug with tail index not properly rendered
        boolean debugDrawHeadTail = false;
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            //wrap tiles when position is outside of map
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap <= 0) indexWrap += spline.path.length;
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.index) {
                if (spline.color != null) {
                    //z = linearVelocity [0 - max box2d] then  hyperdrive velocity
                    float velocity = p.z / (B2DPhysicsSystem.getVelocityLimit() * B2DPhysicsSystem.getVelocityLimit());
                    tmpColor.set(1-velocity, velocity, velocity, 1);
                    if (velocity > 1.01f) {
                        //hyperdrive travel
                        tmpColor.set(1, 1, 1, 1);
                    }
                    shape.line(p.x, p.y, p2.x, p2.y, tmpColor, tmpColor);
                }
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
        }
    }
    
    public void renderPath(SplineComponent spline) {
        //todo: bug with tail index not properly rendered
        boolean debugDrawHeadTail = false;
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            //wrap tiles when position is outside of map
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap <= 0) indexWrap += spline.path.length;
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.index) {
                if (spline.color != null) {
                    //solid color
                    shape.line(p.x, p.y, p2.x, p2.y, spline.color, spline.color);
                } else {
                    //point to point color
                    shape.line(p.x, p.y, p2.x, p2.y, Color.BLUE, Color.GREEN);
                }
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
        }
    }
    
    public void renderRainbowPath(SplineComponent spline) {
        //todo: rainbow render
        //xy mode
        //change of angle: same color when going straight, 360 otherwise, doesnt care speed
        //change of speed: 0-max, doesn't care angle
        boolean debugDrawHeadTail = false;
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            //wrap tiles when position is outside of map
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap <= 0) indexWrap += spline.path.length;
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.index) {
                shape.line(p.x, p.y, p2.x, p2.y, Color.WHITE, Color.GREEN);
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
        }
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
