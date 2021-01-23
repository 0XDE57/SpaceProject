package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.DodgeComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.RenderOrder;
import com.spaceproject.generation.BodyFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class CannonSystem extends IteratingSystem {
    
        super(Family.all(CannonComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CannonComponent cannon = Mappers.cannon.get(entity);
        
         refillAmmo(cannon);
         
         ControllableComponent control = Mappers.controllable.get(entity);
         DodgeComponent dodgeComp = Mappers.dodge.get(entity);
         ShieldComponent shield = Mappers.shield.get(entity);
         boolean canShoot = dodgeComp == null && shield == null;
         if (control.attack && canShoot) {
            fireCannon(cannon, entity);
         }
    }
    
    private void fireCannon(CannonComponent cannon, Entity parentEntity) {
        if (GameScreen.isDebugMode) {
            //Cheat for debug: fast firing and infinite ammo
            //cannon.curAmmo = cannon.maxAmmo;
            //cannon.timerFireRate.setLastEvent(0);
        }
        
        //check if can fire before shooting
        if (!(cannon.curAmmo > 0 && cannon.timerFireRate.canDoEvent()))
            return;
        
        //reset timer if ammo is full, to prevent instant recharge on next shot
        if (cannon.curAmmo == cannon.maxAmmo) {
            cannon.timerRechargeRate.reset();
        }
        
        //create missile
        Entity missile = createMissile(cannon, parentEntity);
        getEngine().addEntity(missile);
        
        //subtract ammo
        --cannon.curAmmo;
        
        //reset timer
        cannon.timerFireRate.reset();
    }
    
    private static void refillAmmo(CannonComponent cannon) {
        if (cannon.curAmmo < cannon.maxAmmo && cannon.timerRechargeRate.canDoEvent()) {
            cannon.curAmmo++;
            cannon.timerRechargeRate.reset();
        }
    }
    
    public static Entity createMissile(CannonComponent cannon, Entity parentEntity) {
        Entity entity = new Entity();
        
        //create texture
        EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateProjectile();
        texture.scale = engineCFG.bodyScale;
        
        //physics
        TransformComponent parentTransform = Mappers.transform.get(parentEntity);
        Vector2 spawnPos = cannon.anchorVec.cpy().rotateRad(parentTransform.rotation).add(parentTransform.pos);
        float rot = parentTransform.rotation + cannon.aimAngle;
        Vector2 sourceVel = Mappers.physics.get(parentEntity).body.getLinearVelocity();
        Vector2 projectileVel = MyMath.vector(rot, cannon.velocity).add(sourceVel);
        float bodyWidth = texture.texture.getWidth() * texture.scale;
        float bodyHeight = texture.texture.getHeight() * texture.scale;
        
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createRect(spawnPos.x, spawnPos.y, bodyWidth, bodyHeight, BodyDef.BodyType.DynamicBody);
        physics.body.setTransform(spawnPos, rot);
        physics.body.setLinearVelocity(projectileVel);
        physics.body.setBullet(true);//turn on CCD
        physics.body.setUserData(entity);
        
        //transform
        TransformComponent transform = new TransformComponent();
        transform.pos.set(physics.body.getPosition());
        transform.rotation = physics.body.getAngle();
        transform.zOrder = RenderOrder.PROJECTILES.getHierarchy();
        
        //expire time (self destruct)
        ExpireComponent expire = new ExpireComponent();
        expire.time = 5;//in seconds ~approx
        
        //missile damage
        DamageComponent missile = new DamageComponent();
        missile.damage = cannon.damage;
        missile.source = parentEntity;
        
        
        entity.add(missile);
        entity.add(expire);
        entity.add(texture);
        entity.add(physics);
        entity.add(transform);
        
        return entity;
    }
    
}
