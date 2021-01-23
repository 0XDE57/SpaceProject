package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
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
import com.spaceproject.config.RenderOrder;
import com.spaceproject.generation.BodyFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class GrowCannonSystem extends IteratingSystem {
    
    final int fireRateMinChargeMS = 60;
    
    public GrowCannonSystem() {
        super(Family.all(GrowCannonComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        GrowCannonComponent growCannon = Mappers.growCannon.get(entity);
        
        if (growCannon.isCharging) {
            //update position to be in front of ship
            updateChargePosition(entity, growCannon);
        
            //accumulate size
            growCharge(growCannon);
        }
        
        //use of shield will cancel grow
        ShieldComponent shieldComponent = Mappers.shield.get(entity);
        if (shieldComponent != null && growCannon.isCharging) {
            //kill charge, cancel shot
            deactivate(growCannon);
            return;
        }
        
        ControllableComponent control = Mappers.controllable.get(entity);
        if (control.attack) {
            if (!growCannon.isCharging) {
                activate(growCannon);
            }
        } else {
            //release
            if (growCannon.growRateTimer.timeSinceLastEvent() < fireRateMinChargeMS) {
                //kill charge, cancel shot
                deactivate(growCannon);
            } else {
                if (growCannon.isCharging) {
                    releaseProjectile(entity, growCannon);
                }
            }
        }
    }
    
    private void growCharge(GrowCannonComponent growCannon) {
        growCannon.size = growCannon.growRateTimer.ratio() * growCannon.maxSize;
        updateChargeTexture(growCannon);
    }
    
    private void updateChargeTexture(GrowCannonComponent growCannon) {
        TextureComponent textureComp = Mappers.texture.get(growCannon.projectileEntity);
        textureComp.scale = growCannon.size;
    }
    
    private void updateChargePosition(Entity parentEntity, GrowCannonComponent growCannon) {
        TransformComponent parentTransform = Mappers.transform.get(parentEntity);
        TransformComponent projectileTransform = Mappers.transform.get(growCannon.projectileEntity);
        Vector2 spawnPos = growCannon.anchorVec.cpy().rotateRad(parentTransform.rotation).add(parentTransform.pos);
        projectileTransform.pos.set(spawnPos);
        projectileTransform.rotation = parentTransform.rotation + growCannon.aimAngle;
    }
    
    private void activate(GrowCannonComponent growCannon) {
        growCannon.isCharging = true;
        growCannon.growRateTimer.reset();
        
        growCannon.projectileEntity = createGrowMissileChargeEntity();
        getEngine().addEntity(growCannon.projectileEntity);
    }
    
    private void deactivate(GrowCannonComponent growCannon) {
        if (growCannon.projectileEntity != null) {
            growCannon.projectileEntity.add(new RemoveComponent());
            growCannon.projectileEntity = null;
        }
        growCannon.isCharging = false;
    }
    
    private void releaseProjectile(Entity parentEntity, GrowCannonComponent growCannon) {
        growCannon.isCharging = false;
        
        //ensure minimum size and update
        growCannon.size = Math.max(growCannon.minSize, growCannon.size);//cap minimum
        updateChargeTexture(growCannon);
        
        //damage modifier
        DamageComponent damageComponent = new DamageComponent();
        damageComponent.source = parentEntity;
        damageComponent.damage = growCannon.baseDamage + (10 * (growCannon.size / growCannon.maxSize) * growCannon.baseDamage);
        if (growCannon.size >= growCannon.maxSize) {
            damageComponent.damage *= 1.15;//bonus damage for maxed out
        }
        growCannon.projectileEntity.add(damageComponent);
        
        //physics
        TextureComponent textureComponent = Mappers.texture.get(growCannon.projectileEntity);
        float bodyWidth = textureComponent.texture.getWidth() * textureComponent.scale;
        float bodyHeight = textureComponent.texture.getHeight() * textureComponent.scale;
        TransformComponent transformComponent = Mappers.transform.get(growCannon.projectileEntity);
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createRect(transformComponent.pos.x, transformComponent.pos.y, bodyWidth, bodyHeight, BodyDef.BodyType.DynamicBody);
        physics.body.setTransform(transformComponent.pos, transformComponent.rotation);
        
        Body parentBody = Mappers.physics.get(parentEntity).body;
        Vector2 projectileVel = MyMath.vector(transformComponent.rotation, growCannon.velocity).add(parentBody.getLinearVelocity());
        physics.body.setLinearVelocity(projectileVel);
        physics.body.setBullet(true);//turn on CCD
        physics.body.setUserData(growCannon.projectileEntity);
        growCannon.projectileEntity.add(physics);
        
        ExpireComponent expire = new ExpireComponent();
        expire.time = 5;
        growCannon.projectileEntity.add(expire);
        
        //release
        growCannon.projectileEntity = null;
    }
    
    private Entity createGrowMissileChargeEntity() {
        Entity entity = new Entity();
        
        //create texture
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateProjectile();
        texture.scale = 0;//start at nothing
        
        TransformComponent transform = new TransformComponent();
        transform.zOrder = RenderOrder.PROJECTILES.getHierarchy();
        
        entity.add(texture);
        entity.add(transform);
        
        return entity;
    }
    
}
