package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.SplineComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class SplineRenderSystem extends IteratingSystem implements Disposable {
        
    private final ShapeRenderer shape;
    private int pathSize = 1000;
    
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
        renderTail(spline);
    }
    
    //see also: https://libgdx.com/wiki/math-utils/path-interface-and-splines
    public void updateTail(SplineComponent spline, TransformComponent transform) {
        if (spline.path == null) {
            //init
            spline.path = new Vector2[pathSize];
            for (int v = 0; v < pathSize; v++) {
                spline.path[v] = new Vector2();
            }
        }
        
        //
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
    
    public void renderTail(SplineComponent spline) {
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector2 p = spline.path[i];
            //shape.point(p.x, p.y, 0);
            
            //wrap tiles when position is outside of map
            int indexWrap = i + 1 % spline.path.length;
            if (indexWrap < 0) indexWrap += spline.path.length;
            Vector2 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.index) {
                if (spline.color != null) {
                    shape.line(p.x, p.y, p2.x, p2.y, spline.color, spline.color);
                } else {
                    //default
                    shape.line(p.x, p.y, p2.x, p2.y, Color.BLUE, Color.GREEN);
                }
            }
        }
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }
    
}
