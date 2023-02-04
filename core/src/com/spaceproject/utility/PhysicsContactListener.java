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
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AsteroidBeltComponent;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.CamTargetComponent;
import com.spaceproject.components.CargoComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.ItemDropComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.RingEffectComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TrailComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.StarComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.ControllerInputSystem;
import com.spaceproject.systems.SoundSystem;


public class PhysicsContactListener implements ContactListener {
    
    private final Engine engine;

    private final int asteroidDamageThreshold = 15000; //impulse threshold to apply damage caused by impact
    private final float asteroidBreakOrbitThreshold = 250;
    private final float vehicleDamageThreshold = 15; //impulse threshold to apply damage to vehicles
    private final float impactMultiplier = 0.1f; //how much damage relative to impulse
    private final float heatDamageRate = 20f;// how quickly stars to damage to health
    private float peakImpulse = 0; //highest recorded impact, stat just to gauge
    
    public PhysicsContactListener(Engine engine) {
        this.engine = engine;
    }
    
    //region contact
    @Override
    public void beginContact(Contact contact) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        if (dataA == null || dataB == null) return;

        onCollision(contact, (Entity) dataA, (Entity) dataB);
    }
    
    private void onCollision(Contact contact, Entity a, Entity b) {
        //check damage collision
        HealthComponent healthA = Mappers.health.get(a);
        if (healthA != null) {
            DamageComponent damageB = Mappers.damage.get(b);
            if (damageB != null) {
                handleDamage(contact, b, a, damageB, healthA);
            }
        }
        HealthComponent healthB = Mappers.health.get(b);
        if (healthB != null) {
            DamageComponent damageA = Mappers.damage.get(a);
            if (damageA != null) {
                handleDamage(contact, a, b, damageA, healthB);
            }
        }
        //check item collision
        ItemDropComponent itemDropA = Mappers.itemDrop.get(a);
        if (itemDropA != null) {
            CargoComponent cargoB = Mappers.cargo.get(b);
            if (cargoB != null) {
                collectItemDrop(contact.getFixtureB(), cargoB, a);
            }
        }
        ItemDropComponent itemDropB = Mappers.itemDrop.get(b);
        if (itemDropB != null) {
            CargoComponent cargoA = Mappers.cargo.get(a);
            if (cargoA != null) {
                collectItemDrop(contact.getFixtureA(), cargoA, b);
            }
        }
    }
    
    private void handleDamage(Contact contact, Entity damageEntity, Entity attackedEntity, DamageComponent damageComponent, HealthComponent healthComponent) {
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
            //attackedEntity.add(new CamTargetComponent());
            
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
        healthComponent.lastHitTime = GameScreen.getGameTimeCurrent();
        healthComponent.lastHitSource = damageEntity;//damageComponent.source?
        
        //remove entity (kill)
        if (healthComponent.health <= 0) {
            //if entity was part of a cluster, remove all entities attached to cluster
            Array<Entity> cluster = ECSUtil.getAttachedEntities(engine, attackedEntity);
            for (Entity e : cluster) {
                e.add(new RemoveComponent());
            }

            /*
            //if entity was charging a projectile, make sure the projectile entity is also removed
            ChargeCannonComponent chargeCannon = Mappers.chargeCannon.get(attackedEntity);
            if (chargeCannon != null && chargeCannon.projectileEntity != null) {
                //destroy or release
                chargeCannon.projectileEntity.add(new RemoveComponent());
            }*/
            
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
        
        //add projectile ghost (fx)
        explodeProjectile(contact, damageEntity, attackedEntity, healthComponent,true);
        
        //remove projectile
        damageEntity.add(new RemoveComponent());
    }
    
    private void collectItemDrop(Fixture collectorFixture, CargoComponent cargo, Entity item) {
        if (collectorFixture.getFilterData().categoryBits != 1)
            return;
        
        //collect
        cargo.count++;
        cargo.lastCollectTime = GameScreen.getGameTimeCurrent();
        item.add(new RemoveComponent());
    }
    
    @Override
    public void endContact(Contact contact) {}
    
    /** NOTE: Must be updated per physics step to retain accurate world!
     * kind of a pseudo entity system to keep physics stepping and collision management separate. */
    public void updateActiveContacts(World world, float deltaTime) {
        for (Contact contact : world.getContactList()) {
            if (!contact.isTouching()) continue;
            
            Object dataA = contact.getFixtureA().getBody().getUserData();
            Object dataB = contact.getFixtureB().getBody().getUserData();
            if (dataA == null || dataB == null) continue;
            
            Entity entityA = (Entity) dataA;
            Entity entityB = (Entity) dataB;
            
            //active heat damage
            StarComponent starA = Mappers.star.get(entityA);
            if (starA != null) {
                doActiveHeatDamage(entityA, entityB, deltaTime);
            }
            StarComponent starB = Mappers.star.get(entityB);
            if (starB != null) {
                doActiveHeatDamage(entityB, entityA, deltaTime);
            }
            //active item movement
            ItemDropComponent itemDropA = Mappers.itemDrop.get(entityA);
            if (itemDropA != null) {
                updateItemAttraction(entityA, entityB);
            }
            ItemDropComponent itemDropB = Mappers.itemDrop.get(entityB);
            if (itemDropB != null) {
                updateItemAttraction(entityB, entityA);
            }
        }
    }
    
    private void doActiveHeatDamage(Entity starEntity, Entity burningEntity, float deltaTime) {
        HealthComponent healthComponent = Mappers.health.get(burningEntity);
        if (healthComponent == null) {
            //check if projectile
            //DamageComponent, remove bullet? or could melt bullet?
            //could set bullet on fire so its more powerful on the other side
            return;
        }
        
        //shield protects from damage
        ShieldComponent shield = Mappers.shield.get(burningEntity);
        if ((shield != null) && (shield.state == ShieldComponent.State.on)) {
            //todo: maybe shield can overheat and start turning red
            // when shield is fully overheated shield will break
            // shield can have heat resistance multiplier that you can upgrade
            return;
        }
        
        //do heat damage dps
        float damage = heatDamageRate * deltaTime;
        //todo: hull heat resistance that can be upgraded: move health to hull?
        healthComponent.health -= damage;
        healthComponent.lastHitTime = GameScreen.getGameTimeCurrent();
        healthComponent.lastHitSource = starEntity;
        
        //remove entity (kill)
        if (healthComponent.health <= 0) {
            //if entity was part of a cluster, remove all entities attached to cluster
            Array<Entity> cluster = ECSUtil.getAttachedEntities(engine, burningEntity);
            for (Entity e : cluster) {
                e.add(new RemoveComponent());
            }
            
            //if entity was asteroid, shatter
            AsteroidComponent asteroid = Mappers.asteroid.get(burningEntity);
            if (asteroid != null) {
                asteroid.doShatter = true;
                engine.getSystem(SoundSystem.class).asteroidShatter();//todo -> asteroidBurn()
            }
            
            Gdx.app.log(getClass().getSimpleName(),
                    "[" + DebugUtil.objString(burningEntity) + "] killed by: star");
        }
    }
    
    private void updateItemAttraction(Entity entityItem, Entity entityCollector) {
        CargoComponent cargoCollector = Mappers.cargo.get(entityCollector);
        if (cargoCollector == null) return;
        
        PhysicsComponent physicsItem = Mappers.physics.get(entityItem);
        //float angleRad = physicsItem.body.getPosition().angleRad(Mappers.physics.get(entityCollector).body.getPosition());
        float angleRad = MyMath.angleTo(Mappers.physics.get(entityCollector).body.getPosition(), physicsItem.body.getPosition());
        physicsItem.body.applyForceToCenter(MyMath.vector(angleRad, 20), true);
    }
    //endregion
    
    //region solve
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {}
    
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
            health.lastHitTime = GameScreen.getGameTimeCurrent();
            if (health.health <= 0) {
                asteroid.doShatter = true;
                entity.add(new RemoveComponent());
                engine.getSystem(SoundSystem.class).asteroidShatter();//warning: coupling
                //Gdx.app.debug(this.getClass().getSimpleName(), "ASTEROID shatter: " + impulse + " -> damage: " + relativeDamage);
            }
        }
    }
    
    private void doVehicleDamage(Entity entity, float impulse) {
        //calc damage relative to how hard impact impulse was
        float damageMultiplier = 0.4f;
        float relativeDamage = (impulse * damageMultiplier);
        
        SoundSystem sound = engine.getSystem(SoundSystem.class);
    
        //don't apply damage while shield active
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state == ShieldComponent.State.on) {
            shield.lastHit = GameScreen.getGameTimeCurrent();
            //todo: break shield if impact is hard enough
            //int shieldBreakThreshold = 500;
            //if (impulse > shieldBreakThreshold) { }
        
            sound.shieldImpact(1);
            
            Gdx.app.debug(getClass().getSimpleName(), "impulse: " + impulse + " -> " + relativeDamage +  " - <shield protect>");
            return; //protected by shield
        }
    
        //impact sound light and hull scrape
        if (impulse < vehicleDamageThreshold) {
            if (impulse > 1) {
                sound.hullImpactLight(impulse / vehicleDamageThreshold);
            } else {
                //todo: scrapping dragging hull across asteroid
                //float friction = contact.getFriction();
                //float tangent = contact.getTangentSpeed();
            }
            return;
        }
        
        //do damage
        HealthComponent health = Mappers.health.get(entity);
        if (health != null) {
            health.health -= relativeDamage;
            health.lastHitTime = GameScreen.getGameTimeCurrent();
            if (health.health <= 0) {
                entity.add(new RemoveComponent());
                Gdx.app.debug(this.getClass().getSimpleName(), "vehicle destroyed: " + impulse + " -> damage: " + relativeDamage);
            } else {
                Gdx.app.debug(this.getClass().getSimpleName(), "high impact damage: " + impulse + " -> -" + relativeDamage);
            }
        }
        
        ControlFocusComponent controlled = Mappers.controlFocus.get(entity);
        if (controlled != null) {
            //warning: coupling
            engine.getSystem(ControllerInputSystem.class).vibrate(100, 1f);
            engine.getSystem(CameraSystem.class).impact(entity);
        }
        //todo: should we hear only controlled, or AI if close enough
        sound.hullImpactHeavy(1);
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
            TrailComponent transferred = (TrailComponent) ECSUtil.transferComponent(entityHit, contactP, TrailComponent.class);
            if (transferred != null) {
                transferred.color = new Color(0, 0, 0, 0.15f);
                AsteroidComponent asteroid = Mappers.asteroid.get(attackedEntity);
                if (asteroid != null) {
                    
                    //only color when asteroid destroyed
                    if (health.health <= 0) {
                        transferred.color.set(asteroid.color).a = 0.75f;
                    }
                }
                transferred.style = TrailComponent.Style.solid;
            }
        }
        
        engine.addEntity(contactP);
    }
    
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        if (dataA == null || dataB == null) return;
        
        Entity entityA = (Entity) dataA;
        Entity entityB = (Entity) dataB;
        
        //get largest impulse from impulse resolution
        float maxImpulse = 0;
        for (float normal : impulse.getNormalImpulses()) {
            maxImpulse = Math.max(maxImpulse, normal);
        }
        peakImpulse = Math.max(maxImpulse, peakImpulse);
        
        //check for asteroid
        AsteroidComponent asteroidA = Mappers.asteroid.get(entityA);
        if (asteroidA != null) {
            asteroidImpact(entityA, asteroidA, maxImpulse);
        }
        AsteroidComponent asteroidB = Mappers.asteroid.get(entityB);
        if (asteroidB != null) {
            asteroidImpact(entityB, asteroidB, maxImpulse);
        }
        
        //check for vehicle
        VehicleComponent vehicleA = Mappers.vehicle.get(entityA);
        if (vehicleA != null) {
            doVehicleDamage(entityA, maxImpulse);
        }
        VehicleComponent vehicleB = Mappers.vehicle.get(entityB);
        if (vehicleB != null) {
            doVehicleDamage(entityB, maxImpulse);
        }
    }
    //endregion
    
}
