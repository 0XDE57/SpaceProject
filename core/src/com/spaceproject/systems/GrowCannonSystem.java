package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.GrowCannonComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.BodyFactory;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class GrowCannonSystem extends IteratingSystem {
    
    public GrowCannonSystem() {
        super(Family.all(GrowCannonComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        manageGrowCannon(entity);
    }
    
    private void manageGrowCannon(Entity entity) {
        GrowCannonComponent growCannon = Mappers.growCannon.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
        
        //canShoot = shield activate cancels grow
        ShieldComponent shieldComponent = Mappers.shield.get(entity);
        if (shieldComponent != null && growCannon.projectileEntity != null) {
            //kill ghost, cancel shot
            growCannon.projectileEntity.add(new RemoveComponent());
            growCannon.projectileEntity = null;
            growCannon.isCharging = false;
        }
        
        
        if (growCannon.isCharging) {
            //update position to be in front of ship
            Entity projectile = growCannon.projectileEntity;
            TransformComponent transformComponent = projectile.getComponent(TransformComponent.class);
            Vector2 spawnPos = growCannon.anchorVec.cpy().rotateRad(transform.rotation).add(transform.pos);
            transformComponent.pos.set(spawnPos);
            transformComponent.rotation = transform.rotation + growCannon.aimAngle;
            
            //accumulate size
            growCannon.size = growCannon.growRateTimer.ratio() * growCannon.maxSize;
            
            TextureComponent textureComp = projectile.getComponent(TextureComponent.class);
            textureComp.scale = growCannon.size;
            
            //release
            if (!control.attack) {
                int fireRateMinChargeMS = 60;
                if (growCannon.growRateTimer.timeSinceLastEvent() < fireRateMinChargeMS) {
                    //kill ghost, cancel shot
                    growCannon.projectileEntity.add(new RemoveComponent());
                    growCannon.projectileEntity = null;
                    growCannon.isCharging = false;
                } else {
                    releaseProjectile(entity, growCannon, projectile, transformComponent, spawnPos, textureComp);
                }
            }
        } else {
            if (control.attack) {
                growCannon.isCharging = true;
                growCannon.growRateTimer.reset();
                
                growCannon.projectileEntity = EntityFactory.createGrowMissileGhost();
                getEngine().addEntity(growCannon.projectileEntity);
            }
        }
    }
    
    private void releaseProjectile(Entity entity, GrowCannonComponent growCannon, Entity projectile, TransformComponent transformComponent, Vector2 spawnPos, TextureComponent textureComp) {
        growCannon.isCharging = false;
        
        growCannon.size = Math.max(growCannon.minSize, growCannon.size);//cap minimum
        textureComp.scale = growCannon.size;
        
        //damage modifier
        DamageComponent damageComponent = new DamageComponent();
        damageComponent.source = entity;
        damageComponent.damage = growCannon.baseDamage + ((growCannon.size / growCannon.maxSize) * growCannon.baseDamage);
        if (growCannon.size >= growCannon.maxSize) {
            damageComponent.damage *= 1.15;//bonus damage for maxed out
        }
        Gdx.app.log("shoot", damageComponent.damage + "");
        projectile.add(damageComponent);
        
        //physics
        PhysicsComponent physics = new PhysicsComponent();
        float bodyWidth = textureComp.texture.getWidth() * textureComp.scale;
        float bodyHeight = textureComp.texture.getHeight() * textureComp.scale;
        physics.body = BodyFactory.createRect(spawnPos.x, spawnPos.y, bodyWidth, bodyHeight, BodyDef.BodyType.DynamicBody);
        physics.body.setTransform(spawnPos, transformComponent.rotation);
        
        Body parentBody = Mappers.physics.get(entity).body;
        Vector2 projectileVel = MyMath.vector(transformComponent.rotation, growCannon.velocity).add(parentBody.getLinearVelocity());
        physics.body.setLinearVelocity(projectileVel);
        physics.body.setBullet(true);//turn on CCD
        physics.body.setUserData(projectile);
        projectile.add(physics);
        
        ExpireComponent expire = new ExpireComponent();
        expire.time = 5;
        projectile.add(expire);
        
        //release
        growCannon.projectileEntity = null;
    }
    
}
