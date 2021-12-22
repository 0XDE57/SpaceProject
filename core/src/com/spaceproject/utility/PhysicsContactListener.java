package com.spaceproject.utility;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.CamTargetComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.Sprite3DComponent;

public class PhysicsContactListener implements ContactListener {
    
    private final Engine engine;
    private final int highImpactThreshold = 15000;
    private float peakImpulse = 0;
    
    public PhysicsContactListener(Engine engine) {
        this.engine = engine;
    }
    
    @Override
    public void beginContact(Contact contact) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        
        if (dataA != null && dataB != null) {
            onCollision((Entity) dataA, (Entity) dataB);
        }
    }
    
    @Override
    public void endContact(Contact contact) {}
    
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {}
    
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        float maxImpulse = 0;
        for (float normal : impulse.getNormalImpulses()) {
            maxImpulse = Math.max(maxImpulse, normal);
        }
        peakImpulse = Math.max(maxImpulse, peakImpulse);
        
        if (maxImpulse > highImpactThreshold) {
            onHighImpulseImpact(contact, maxImpulse);
        }
    }
    
    private void onHighImpulseImpact(Contact contact, float impulse) {
        Gdx.app.debug(this.getClass().getSimpleName(), "collide " + impulse + " : " + peakImpulse);
        
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        if (dataA != null && dataB != null) {
            Entity entityA = (Entity) dataA;
            Entity entityB = (Entity) dataB;
            AsteroidComponent asteroidA = Mappers.asteroid.get(entityA);
            AsteroidComponent asteroidB = Mappers.asteroid.get(entityB);
            if (asteroidA != null && asteroidB != null) {
                asteroidA.doShatter = true;
                asteroidB.doShatter = true;
                
                //calc damage relative to size of bodies and impact impulse
                float damage = impulse * 0.1f;
                float total = asteroidA.area + asteroidB.area;
                float damageA = damage * (asteroidA.area / total);
                float damageB = damage * (asteroidB.area / total);

                //todo: how should shatter mechanics handle? what if pass down extra damage to children.
                // eg: asteroid has 100 hp, breaks into 2 = 50hp children
                //   damage 110 = -10 hp, breaks into 2 minus 5 each = 45hp
                //   damage 200 = -100 hp, breaks into 2 minus 50 = 0hp = don't spawn children -> instant destruction
                //  could play with different rules and ratios, find what feels good
                
                HealthComponent healthA = Mappers.health.get(entityA);
                HealthComponent healthB = Mappers.health.get(entityB);
                healthA.health -= damageA;
                healthB.health -= damageB;
                
                if (healthA.health <= 0) {
                    entityA.add(new RemoveComponent());
                    Gdx.app.debug(this.getClass().getSimpleName(), "ASTEROID A break down " + impulse + " : " + damageA);
                }
                if (healthB.health <= 0) {
                    entityB.add(new RemoveComponent());
                    Gdx.app.debug(this.getClass().getSimpleName(), "ASTEROID B break down " + impulse + " : " + damageB);
                }
            }
        }
    }
    
    private void onCollision(Entity a, Entity b) {
        //todo: collision filtering: http://www.iforce2d.net/b2dtut/collision-filtering
        DamageComponent damageA = Mappers.damage.get(a);
        DamageComponent damageB = Mappers.damage.get(b);
        HealthComponent healthA = Mappers.health.get(a);
        HealthComponent healthB = Mappers.health.get(b);
        
        if (damageA != null && healthB != null) {
            onAttacked(a, b, damageA, healthB);
        }
        if (damageB != null && healthA != null) {
            onAttacked(b, a, damageB, healthA);
        }
    }
    
    private void onAttacked(Entity damageEntity, Entity attackedEntity, DamageComponent damageComponent, HealthComponent healthComponent) {
        if (damageComponent.source == attackedEntity) {
            return; //ignore self-inflicted damage
        }
        Gdx.app.debug(this.getClass().getSimpleName(),
                "[" + DebugUtil.objString(attackedEntity) + "] attacked by: [" + DebugUtil.objString(damageComponent.source) + "]");
        
        //check if attacked entity was AI
        AIComponent ai = Mappers.AI.get(attackedEntity);
        if (ai != null) {
            //focus camera on target
            attackedEntity.add(new CamTargetComponent());
            
            //focus ai on player
            ai.attackTarget = damageComponent.source;
            ai.state = AIComponent.State.attack;
        } else if (Mappers.controlFocus.get(damageEntity) != null) {
            //someone attacked player, focus on enemy
            damageEntity.add(new CamTargetComponent());
        }
        
        //check for shield
        ShieldComponent shieldComp = Mappers.shield.get(attackedEntity);
        if ((shieldComp != null) && (shieldComp.state == ShieldComponent.State.on)) {
            //todo: "break effect", sound effect, particle effect
            //shieldComp.state == ShieldComponent.State.break;??
            damageEntity.add(new RemoveComponent());
            return;
        }
    
        //add roll to hit body
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(attackedEntity);
        if (sprite3D != null) {
            float roll = 50 * MathUtils.degRad;
            sprite3D.renderable.angle += MathUtils.randomBoolean() ? roll : -roll;
        }
        
        
        //do damage
        healthComponent.health -= damageComponent.damage;
        
        //remove entity (kill)
        if (healthComponent.health <= 0) {
            //if entity was part of a cluster, remove all entities attached to cluster
            Array<Entity> cluster = ECSUtil.getAttachedEntities(engine, attackedEntity);
            for (Entity e : cluster) {
                e.add(new RemoveComponent());
            }
            
            //if entity was charging a projectile, make sure the projectile entity is also removed
            ChargeCannonComponent chargeCannon = Mappers.chargeCannon.get(attackedEntity);
            if (chargeCannon != null && chargeCannon.projectileEntity != null) {
                //destroy or release
                chargeCannon.projectileEntity.add(new RemoveComponent());
            }
    
            //if entity was asteroid, shatter
            AsteroidComponent asteroidA = Mappers.asteroid.get(attackedEntity);
            if (asteroidA != null) {
                asteroidA.doShatter = true;
            }
            
            Gdx.app.log(this.getClass().getSimpleName(),
                    "[" + DebugUtil.objString(attackedEntity) + "] killed by: [" + DebugUtil.objString(damageComponent.source) + "]");
        }
        
        //remove projectile
        damageEntity.add(new RemoveComponent());
    }
    
}
