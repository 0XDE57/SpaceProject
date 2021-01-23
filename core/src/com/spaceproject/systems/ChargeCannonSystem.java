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
import com.spaceproject.components.ChargeCannonComponent;
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

public class ChargeCannonSystem extends IteratingSystem {
    
    final int fireRateMinChargeMS = 60;
    
    public ChargeCannonSystem() {
        super(Family.all(ChargeCannonComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ChargeCannonComponent chargeCannon = Mappers.chargeCannon.get(entity);
        
        if (chargeCannon.isCharging) {
            //update position to be in front of ship
            updateChargePosition(chargeCannon, entity);
        
            //accumulate size
            growCharge(chargeCannon);
        }
        
        //use of shield will cancel charge
        ShieldComponent shieldComponent = Mappers.shield.get(entity);
        if (shieldComponent != null && chargeCannon.isCharging) {
            //kill charge, cancel shot
            deactivate(chargeCannon);
            return;
        }
        
        ControllableComponent control = Mappers.controllable.get(entity);
        if (control.attack) {
            if (!chargeCannon.isCharging) {
                activate(chargeCannon);
            }
        } else {
            //release
            if (chargeCannon.growRateTimer.timeSinceLastEvent() < fireRateMinChargeMS) {
                //kill charge, cancel shot
                deactivate(chargeCannon);
            } else {
                if (chargeCannon.isCharging) {
                    releaseProjectile(chargeCannon, entity);
                }
            }
        }
    }
    
    private void growCharge(ChargeCannonComponent chargeCannon) {
        chargeCannon.size = chargeCannon.growRateTimer.ratio() * chargeCannon.maxSize;
        updateChargeTexture(chargeCannon);
    }
    
    private void updateChargeTexture(ChargeCannonComponent chargeCannon) {
        TextureComponent textureComp = Mappers.texture.get(chargeCannon.projectileEntity);
        textureComp.scale = chargeCannon.size;
    }
    
    private void updateChargePosition(ChargeCannonComponent chargeCannon, Entity parentEntity) {
        TransformComponent parentTransform = Mappers.transform.get(parentEntity);
        TransformComponent projectileTransform = Mappers.transform.get(chargeCannon.projectileEntity);
        Vector2 spawnPos = chargeCannon.anchorVec.cpy().rotateRad(parentTransform.rotation).add(parentTransform.pos);
        projectileTransform.pos.set(spawnPos);
        projectileTransform.rotation = parentTransform.rotation + chargeCannon.aimAngle;
    }
    
    private void activate(ChargeCannonComponent chargeCannon) {
        chargeCannon.isCharging = true;
        chargeCannon.growRateTimer.reset();
        
        chargeCannon.projectileEntity = createGrowMissileChargeEntity();
        getEngine().addEntity(chargeCannon.projectileEntity);
    }
    
    private void deactivate(ChargeCannonComponent chargeCannon) {
        if (chargeCannon.projectileEntity != null) {
            chargeCannon.projectileEntity.add(new RemoveComponent());
            chargeCannon.projectileEntity = null;
        }
        chargeCannon.isCharging = false;
    }
    
    private void releaseProjectile(ChargeCannonComponent chargeCannon, Entity parentEntity) {
        chargeCannon.isCharging = false;
        
        //ensure minimum size and update
        chargeCannon.size = Math.max(chargeCannon.minSize, chargeCannon.size);
        updateChargeTexture(chargeCannon);
        
        //damage modifier
        DamageComponent damageComponent = new DamageComponent();
        damageComponent.source = parentEntity;
        damageComponent.damage = chargeCannon.baseDamage + (10 * (chargeCannon.size / chargeCannon.maxSize) * chargeCannon.baseDamage);
        if (chargeCannon.size >= chargeCannon.maxSize) {
            damageComponent.damage *= 1.15;//bonus damage for maxed out
        }
        chargeCannon.projectileEntity.add(damageComponent);
        
        //physics
        TextureComponent textureComponent = Mappers.texture.get(chargeCannon.projectileEntity);
        float bodyWidth = textureComponent.texture.getWidth() * textureComponent.scale;
        float bodyHeight = textureComponent.texture.getHeight() * textureComponent.scale;
        TransformComponent transformComponent = Mappers.transform.get(chargeCannon.projectileEntity);
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createRect(transformComponent.pos.x, transformComponent.pos.y, bodyWidth, bodyHeight, BodyDef.BodyType.DynamicBody);
        physics.body.setTransform(transformComponent.pos, transformComponent.rotation);
        
        Body parentBody = Mappers.physics.get(parentEntity).body;
        Vector2 projectileVel = MyMath.vector(transformComponent.rotation, chargeCannon.velocity).add(parentBody.getLinearVelocity());
        physics.body.setLinearVelocity(projectileVel);
        physics.body.setBullet(true);//turn on CCD
        physics.body.setUserData(chargeCannon.projectileEntity);
        chargeCannon.projectileEntity.add(physics);
        
        ExpireComponent expire = new ExpireComponent();
        expire.time = 5;
        chargeCannon.projectileEntity.add(expire);
        
        //release
        chargeCannon.projectileEntity = null;
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
