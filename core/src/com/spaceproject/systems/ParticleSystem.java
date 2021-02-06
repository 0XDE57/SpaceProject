package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ParticleSystem extends IteratingSystem implements EntityListener {
    
    SpriteBatch spriteBatch;
    ParticleEffect fireEffect;
    ParticleEffectPool effectPool;
    
    public ParticleSystem() {
        super(Family.all(ParticleComponent.class, TransformComponent.class).get());
        
        spriteBatch = new SpriteBatch();
    
        fireEffect = new ParticleEffect();
        fireEffect.load(Gdx.files.internal("particles/test.particle"), Gdx.files.internal("particles/"));
        fireEffect.scaleEffect(0.02f);
        effectPool = new ParticleEffectPool(fireEffect, 20, 20);
        
        fireEffect.start();
    }
    
    @Override
    public void update(float deltaTime) {
        spriteBatch.setProjectionMatrix(GameScreen.cam.combined);

        spriteBatch.begin();
        super.update(deltaTime);
        spriteBatch.end();
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle.pooledEffect == null) {
            return;
        }
        
        ControllableComponent control = Mappers.controllable.get(entity);
        if (control != null) {
            if (control.moveForward) {
                particle.pooledEffect.start();
            } else {
                particle.pooledEffect.allowCompletion();
            }
        } else {
            particle.pooledEffect.allowCompletion();
        }
        
        TransformComponent transform = Mappers.transform.get(entity);
        Array<ParticleEmitter> emitters = particle.pooledEffect.getEmitters();
        float engineRotation = transform.rotation * MathUtils.radDeg + 180;
        for (int i = 0; i < emitters.size; i++) {
            ParticleEmitter.ScaledNumericValue val = emitters.get(i).getAngle();
            val.setHigh(engineRotation);
            val.setLow(engineRotation);
        }
        particle.offset.setAngleDeg(engineRotation);
        particle.pooledEffect.setPosition(transform.pos.x + particle.offset.x, transform.pos.y + particle.offset.y);
        particle.pooledEffect.draw(spriteBatch, deltaTime);
    }
    
    @Override
    public void entityAdded(Entity entity) {
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null && particle.pooledEffect == null) {
            Gdx.app.log(this.getClass().getSimpleName(), "obtained");
            particle.pooledEffect = effectPool.obtain();
            particle.pooledEffect.allowCompletion();//start with emitter off
        }
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null && particle.pooledEffect != null) {
            Gdx.app.log(this.getClass().getSimpleName(), "free");
            effectPool.free(particle.pooledEffect);
        }
    }
}
