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
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ParticleSystem extends IteratingSystem implements EntityListener {
    
    SpriteBatch spriteBatch;
    ParticleEffect fireEffect;
    ParticleEffectPool fireEffectPool;
    ParticleEffect chargeEffect;
    ParticleEffectPool chargeEffectPool;
    
    
    public ParticleSystem() {
        super(Family.all(ParticleComponent.class, TransformComponent.class).get());
        
        spriteBatch = new SpriteBatch();
    
        fireEffect = new ParticleEffect();
        fireEffect.load(Gdx.files.internal("particles/shipEngineFire.particle"), Gdx.files.internal("particles/"));
        fireEffect.scaleEffect(0.02f);
        fireEffectPool = new ParticleEffectPool(fireEffect, 20, 20);
        
        chargeEffect = new ParticleEffect();
        chargeEffect.load(Gdx.files.internal("particles/absorb2.particle"), Gdx.files.internal("particles/"));
        chargeEffect.scaleEffect(0.02f);
        chargeEffectPool = new ParticleEffectPool(chargeEffect, 20, 20);
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
        
        switch (particle.type) {
            case shipEngine:
                updateEngineParticle(entity, particle);
                break;
            case bulletCharge:
                updateChargeParticle(entity, particle);
                break;
        }
        
        particle.pooledEffect.draw(spriteBatch, deltaTime);
    }
    
    private void updateEngineParticle(Entity entity, ParticleComponent particle) {
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
    }
    
    private void updateChargeParticle(Entity entity, ParticleComponent particle) {
        ChargeCannonComponent cannon = Mappers.chargeCannon.get(entity);
        if (cannon != null) {
            if (cannon.isCharging) {
                particle.pooledEffect.start();
            } else {
                particle.pooledEffect.allowCompletion();
            }
        }
        
        //ensure particles always drift towards current bullet location as if attracted by gravity
        //using wind to modify velocity on the X axis, and gravity for the Y axis
        //  velocityX += (particle.wind + particle.windDiff * windValue.getScale(percent)) * delta;
        //  velocityY += (particle.gravity + particle.gravityDiff * gravityValue.getScale(percent)) * delta;
        TransformComponent transform = Mappers.transform.get(entity);
        Array<ParticleEmitter> emitters = particle.pooledEffect.getEmitters();
        float magnitude = 30; // + (1 * physics.body.getLinearVelocity())?
        float velX = (transform.pos.x - emitters.get(0).getX()) * magnitude;
        float velY = (transform.pos.y - emitters.get(0).getY()) * magnitude;
        
        particle.pooledEffect.setPosition(transform.pos.x, transform.pos.y);
        
        ParticleEmitter.ScaledNumericValue gravity = emitters.get(0).getGravity();
        gravity.setActive(true);
        gravity.setHigh(velY);
        gravity.setLow(velY);
        
        ParticleEmitter.ScaledNumericValue wind = emitters.get(0).getWind();
        wind.setActive(true);
        wind.setHigh(velX);
        wind.setLow(velX);
    }
    
    @Override
    public void entityAdded(Entity entity) {
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null && particle.pooledEffect == null) {
            switch (particle.type) {
                case shipEngine:
                    particle.pooledEffect = fireEffectPool.obtain();
                    //start with emitter off
                    particle.pooledEffect.allowCompletion();
                    break;
                case bulletCharge:
                    particle.pooledEffect = chargeEffectPool.obtain();
                    //start with emitter on
                    particle.pooledEffect.start();
            }
            //Gdx.app.log(this.getClass().getSimpleName(), "obtained");
        }
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null && particle.pooledEffect != null) {
            switch (particle.type) {
                case shipEngine:
                    fireEffectPool.free(particle.pooledEffect);
                    break;
                case bulletCharge:
                    chargeEffectPool.free(particle.pooledEffect);
            }
            //Gdx.app.log(this.getClass().getSimpleName(), "free");
        }
    }
    
}
