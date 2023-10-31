package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.RingEffectComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ProjectileHitRenderSystem extends IteratingSystem implements Disposable {
    //todo: change from iterating to entitystem, get rid of ring component entities and call this system directly to add one from ring pool
    private final ShapeRenderer shape;
    private final float growthRate = 2.0f;
    
    public ProjectileHitRenderSystem() {
        super(Family.all(TransformComponent.class, RingEffectComponent.class).get());
        shape = new ShapeRenderer();
    }
    
    @Override
    public void update(float delta) {
        //update matrix and convert screen coords to world cords.
        //projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(GameScreen.cam.combined);
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        //render
        shape.begin(ShapeRenderer.ShapeType.Line);
        super.update(delta);
        shape.end();
        
        //disable transparency
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        TransformComponent t = Mappers.transform.get(entity);
        RingEffectComponent ring = Mappers.ring.get(entity);
        ring.radius += growthRate * delta;
        shape.setColor(1, 1, 1, 1 - ring.radius);
        shape.circle(t.pos.x, t.pos.y, ring.radius);
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }
    
}
