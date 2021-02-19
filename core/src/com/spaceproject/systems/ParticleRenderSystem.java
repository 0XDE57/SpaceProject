package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ParticleRenderSystem extends IteratingSystem {
    
    private final OrthographicCamera cam;
    private final SpriteBatch spriteBatch;
    
    public ParticleRenderSystem() {
        super(Family.all(ParticleComponent.class).get());
    
        cam = GameScreen.cam;
        spriteBatch = new SpriteBatch();
    }
    
    @Override
    public void update(float deltaTime) {
        //enable transparency
        //todo: for some reason enable transparency seems to help drift a little bit?
        //todo: check against shapeRender exactly like shield
        //Gdx.gl.glEnable(GL20.GL_BLEND);
        //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        super.update(deltaTime);
        spriteBatch.end();
    
        //Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle.pooledEffect == null) {
            return;
        }
        
        particle.pooledEffect.draw(spriteBatch, deltaTime);
    }
}
