package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.SplineComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class SplineRenderSystem extends IteratingSystem implements Disposable {
        
    private final ShapeRenderer shape;
    private final Color tmpColor = new Color();
    private int maxPathSize = 1000;
    boolean debugDrawHeadTail = false;
    
    public SplineRenderSystem() {
        super(Family.all(SplineComponent.class, TransformComponent.class).get());
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
        TransformComponent transform = Mappers.transform.get(entity);
        
        updateTail(spline, transform);
        
        //todo: fade nicely
        renderPath(spline);
        //renderRainbowPath(spline);
    }
    
    //see also: https://libgdx.com/wiki/math-utils/path-interface-and-splines
    public void updateTail(SplineComponent spline, TransformComponent transform) {
        if (spline.path == null) {
            //init
            spline.path = new Vector2[maxPathSize];
            for (int v = 0; v < maxPathSize; v++) {
                spline.path[v] = new Vector2();
            }
        }
        
        //
        //todo: don't record useless points (duplicate). consider resolution
        //if (spline.path[spline.index].epsilonEquals(transform.pos.x, transform.pos.y, 10)) {
            //delta too small skip update
        //    return;
        //}
        
        spline.path[spline.index].set(transform.pos.x, transform.pos.y);
        //roll index
        spline.index++;
        if (spline.index >= spline.path.length) {
            spline.index = 0;
        }
        
    }
    
    public void renderPath(SplineComponent spline) {
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector2 p = spline.path[i];
            
            //wrap tiles when position is outside of map
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap < 0) indexWrap += spline.path.length;
            Vector2 p2 = spline.path[indexWrap];
            
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
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector2 p = spline.path[i];
            
            //wrap tiles when position is outside of map
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap < 0) indexWrap += spline.path.length;
            Vector2 p2 = spline.path[indexWrap];
            
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
