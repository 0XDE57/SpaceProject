package com.spaceproject.utility;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AsteroidBeltComponent;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.CamTargetComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.RingEffectComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.SplineComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.systems.SoundSystem;


public class PhysicsContactListener implements ContactListener {
    
    private final Engine engine;
    private final int asteroidDamageThreshold = 15000; //impulse threshold to apply damage caused by impact
    private final float asteroidBreakOrbitThreshold = 250;
    private final int vehicleDamageThreshold = 15; //impulse threshold to apply damage to vehicles
    private final float impactMultiplier = 0.1f; //how much damage relative to impulse
    private float peakImpulse = 0; //highest recorded impact, stat just to gauge
    
    public PhysicsContactListener(Engine engine) {
        this.engine = engine;
    }
    
    @Override
    public void beginContact(Contact contact) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        if (dataA != null && dataB != null) {
            onCollision(contact, (Entity) dataA, (Entity) dataB);
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
        
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        if (dataA != null && dataB != null) {
            Entity entityA = (Entity) dataA;
            Entity entityB = (Entity) dataB;
            
            AsteroidComponent asteroidA = Mappers.asteroid.get(entityA);
            if (asteroidA != null) {
                asteroidImpact(entityA, asteroidA, maxImpulse);
            }
            AsteroidComponent asteroidB = Mappers.asteroid.get(entityB);
            if (asteroidB != null) {
                asteroidImpact(entityB, asteroidB, maxImpulse);
            }

            if (maxImpulse > vehicleDamageThreshold) {
                VehicleComponent vehicleA = Mappers.vehicle.get(entityA);
                if (vehicleA != null) {
                    doVehicleDamage(entityA, maxImpulse);
                }
                VehicleComponent vehicleB = Mappers.vehicle.get(entityA);
                if (vehicleB != null) {
                    doVehicleDamage(entityB, maxImpulse);
                }
            }
        }
    }
    
    private void asteroidImpact(Entity entity, AsteroidComponent asteroid, float impulse) {
        if (impulse > asteroidBreakOrbitThreshold) {
            if (asteroid.parentOrbitBody != null) {
                AsteroidBeltComponent circumstellar = Mappers.asteroidBelt.get(asteroid.parentOrbitBody);
                //if (circumstellar.spawnTimer != null && circumstellar.spawnTimer.canDoEvent()) {
                if (circumstellar.spawned == circumstellar.maxSpawn) {
                    asteroid.parentOrbitBody = null;
                    //Gdx.app.debug(this.getClass().getSimpleName(), "ASTEROID knocked out of orbit: " + impulse);
                }
            }
        }
        
        if (impulse > asteroidDamageThreshold) {
            //calc damage relative to size of bodies and how hard impact impulse was
            float relativeDamage = (impulse * impactMultiplier) * asteroid.area;
    
            //damage (potential could be optimization to remove health, add merge it with asteroid, one less mapper)
            HealthComponent health = Mappers.health.get(entity);
            health.health -= relativeDamage;
            health.lastHit = GameScreen.getGameTimeCurrent();
            if (health.health <= 0) {
                asteroid.doShatter = true;
                entity.add(new RemoveComponent());
                //warning: coupling
                engine.getSystem(SoundSystem.class).asteroidShatter();
                //Gdx.app.debug(this.getClass().getSimpleName(), "ASTEROID shatter: " + impulse + " -> damage: " + relativeDamage);
            }
        }
    }
    
    private void doVehicleDamage(Entity entity, float impulse) {
        //don't apply damage while shield active
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state == ShieldComponent.State.on) {
            //todo: break shield if impact is hard enough
            //int shieldBreakThreshold = 500;
            //if (impulse > shieldBreakThreshold) { }
            return; //protected by shield
        }
        
        Gdx.app.debug(this.getClass().getSimpleName(), "high impact damage: " + impulse);
        
        //calc damage relative to how hard impact impulse was
        float damageMultiplier = 0.4f;
        float relativeDamage = (impulse * damageMultiplier);
        
        HealthComponent health = Mappers.health.get(entity);
        if (health != null) {
            health.health -= relativeDamage;
            health.lastHit = GameScreen.getGameTimeCurrent();
            if (health.health <= 0) {
                entity.add(new RemoveComponent());
                Gdx.app.debug(this.getClass().getSimpleName(), "vehicle destroyed: " + impulse + " -> damage: " + relativeDamage);
            }
        }
    }
    
    private void onCollision(Contact contact, Entity a, Entity b) {
        //todo: collision filtering: http://www.iforce2d.net/b2dtut/collision-filtering
        DamageComponent damageA = Mappers.damage.get(a);
        DamageComponent damageB = Mappers.damage.get(b);
        HealthComponent healthA = Mappers.health.get(a);
        HealthComponent healthB = Mappers.health.get(b);
        
        if (damageA != null && healthB != null) {
            onAttacked(contact, a, b, damageA, healthB);
        }
        if (damageB != null && healthA != null) {
            onAttacked(contact, b, a, damageB, healthA);
        }
    }
    
    private void onAttacked(Contact contact, Entity damageEntity, Entity attackedEntity, DamageComponent damageComponent, HealthComponent healthComponent) {
        if (damageComponent.source == attackedEntity) {
            return; //ignore self-inflicted damage
        }
        /*
        Gdx.app.debug(this.getClass().getSimpleName(),
                "[" + DebugUtil.objString(attackedEntity) + "] attacked by: [" + DebugUtil.objString(damageComponent.source) + "]");
        */
        
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
        healthComponent.lastHit = GameScreen.getGameTimeCurrent();
        
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
            AsteroidComponent asteroid = Mappers.asteroid.get(attackedEntity);
            if (asteroid != null) {
                asteroid.doShatter = true;
                engine.getSystem(SoundSystem.class).asteroidShatter();
            }
            
            /*
            Gdx.app.log(this.getClass().getSimpleName(),
                    "[" + DebugUtil.objString(attackedEntity) + "] killed by: [" + DebugUtil.objString(damageComponent.source) + "]");
             */
        }
        
        //remove projectile
        damageEntity.add(new RemoveComponent());
    
        //add projectile ghost (fx)
        explodeProjectile(contact, damageEntity, attackedEntity, healthComponent,true);
    }
    
    private void explodeProjectile(Contact contact, Entity entityHit, Entity attackedEntity, HealthComponent health, boolean showGhost) {
        WorldManifold manifold = contact.getWorldManifold();
        //for (Vector2 p : manifold.getPoints()) {
        Vector2 p = manifold.getPoints()[0];
        
        Entity contactP = new Entity();
        
        //create entity at point of contact
        TransformComponent trans = new TransformComponent();
        trans.pos.set(p);
        contactP.add(trans);
        //todo: transfer velocity from object hit
        
        contactP.add(new RingEffectComponent());
        
        ExpireComponent expire = new ExpireComponent();
        expire.timer = new SimpleTimer(2000, true);
        contactP.add(expire);
        
        //todo: better particle this one is ugly
        ParticleComponent particle = new ParticleComponent();
        particle.type = ParticleComponent.EffectType.bulletExplode;
        //contactP.add(particle);
        
        if (showGhost) {
            SplineComponent transferred = (SplineComponent) ECSUtil.transferComponent(entityHit, contactP, SplineComponent.class);
            if (transferred != null) {
                transferred.color = new Color(0, 0, 0, 0.15f);
                AsteroidComponent asteroid = Mappers.asteroid.get(attackedEntity);
                if (asteroid != null) {
                    //only color when asteroid destroyed
                    if (health.health <= 0) {
                        transferred.color.set(asteroid.color).a = 0.75f;
                    }
                }
                transferred.style = SplineComponent.Style.solid;
            }
        }
        
        engine.addEntity(contactP);
    }
    
}
