package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;

// based off:
// http://gafferongames.com/game-physics/fix-your-timestep/
// http://saltares.com/blog/games/fixing-your-timestep-in-libgdx-and-box2d/
public class FixedPhysicsSystem extends EntitySystem implements IRequireGameContext {
    
    private static final int velocityIterations = 6;
    private static final int positionIterations = 2;
    private static final int stepPerFrame = 60;
    private static final float timeStep = 1 / (float) stepPerFrame;
    private static float accumulator = 0f;
    
    //movement limit = 2 * units per step
    //eg step of 60: 60 * 2 = 120,  max vel = 120
    
    private World world;
    
    private ImmutableArray<Entity> entities;
    
    @Override
    public void initContext(GameScreen gameScreen) {
        this.world = gameScreen.world;
        
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        Family family = Family.all(PhysicsComponent.class, TransformComponent.class).get();
        entities = engine.getEntitiesFor(family);
    
        world.setContactListener(new Listener(engine));
    }
    
    @Override
    public void update(float deltaTime) {
        accumulator += deltaTime;
        while (accumulator >= timeStep) {
            //System.out.println("update: " + deltaTime + ". " + accumulator);
            world.step(timeStep, velocityIterations, positionIterations);
            accumulator -= timeStep;
            
            updateTransform();
        }
        
        interpolate(deltaTime, accumulator);
    }
    
    private void updateTransform() {
        for (Entity entity : entities) {
            PhysicsComponent physics = Mappers.physics.get(entity);
            
            if (!physics.body.isActive()) {
                return;
            }
            
            TransformComponent transform = Mappers.transform.get(entity);
            transform.pos.set(physics.body.getPosition());
            transform.rotation = physics.body.getAngle();
            transform.velocity.set(physics.body.getLinearVelocity());
        }
    }
    
    private void interpolate(float deltaTime, float accumulator) {
        /*
        if (physics.body.isActive()) {
            transform.position.x = physics.body.getPosition().x * alpha + old.position.x * (1.0f - alpha);
            transform.position.y = physics.body.getPosition().y * alpha + old.position.y * (1.0f - alpha);
            transform.angle = physics.body.getAngle() * MathUtils.radiansToDegrees * alpha + old.angle * (1.0f - alpha);
        }*/
    }
    
}


class Listener implements ContactListener {
    
    private Engine engine;
    
    public Listener(Engine engine) {
        this.engine = engine;
    }
    
    @Override
    public void beginContact(Contact contact) {
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();
        
        //System.out.println("A: " + Misc.objString(dataA) + " - B: " + Misc.objString(dataB));
        onCollision((Entity)dataA, (Entity)dataB);
    }
    
    @Override
    public void endContact(Contact contact) {
    
    }
    
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    
    }
    
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    
    }
    
    private void onCollision(Entity a, Entity b) {
        //TODO: this should just fire an event for other systems to care about
        //if missile & character: damage character, remove missile
        //if missile & ship: damage ship, remove missile
        //if AI: mark attacked
        //if character & ship: resolve (do nothing here)
        //if ship & ship: resolve (do nothing here)
        
        DamageComponent mA = Mappers.damage.get(a);
        DamageComponent mB = Mappers.damage.get(b);
        HealthComponent hA = Mappers.health.get(a);
        HealthComponent hB = Mappers.health.get(b);
        
        if (mA != null && hB != null) {
            onAttacked(a, b, mA, hB);
        }
        if (mB != null && hA != null) {
            onAttacked(b, a, mB, hA);
        }
        
    }
    
    
    private void onAttacked(Entity damageEntity, Entity attackedEntity, DamageComponent damageComponent, HealthComponent healthComponent) {
        //TODO: move this. collision/physics doesnt care about damage. a combat system should subscribe to this event.
        if (damageComponent.source == attackedEntity) {
            return;
        }
        
        //check for AI
        AIComponent ai = Mappers.AI.get(attackedEntity);
        if (ai != null) {
            ai.attackTarget = damageComponent.source;
            ai.state = AIComponent.testState.attack;
            Gdx.app.log(this.getClass().getSimpleName(), "AI [" + Misc.objString(attackedEntity) + "] attacked by: [" + Misc.objString(damageComponent.source) + "]");
        }
        
        
        //check for shield
        ShieldComponent shieldComp = Mappers.shield.get(attackedEntity);
        if (shieldComp != null) {
            if (shieldComp.active) {
                Vector2 pos = Mappers.transform.get(attackedEntity).pos;
                Circle c = new Circle(pos, shieldComp.radius);
                PhysicsComponent physicsComponent = Mappers.physics.get(attackedEntity);
                //if (PolygonUtil.overlaps(physicsComponent.poly, c)) {
                    attackedEntity.remove(ShieldComponent.class);
                    damageEntity.add(new RemoveComponent());
                    return;
                //}
            }
        }
        
        
        //do damage
        healthComponent.health -= damageComponent.damage;
        
        //remove entity (kill)
        if (healthComponent.health <= 0) {
            attackedEntity.add(new RemoveComponent());
            Gdx.app.log(this.getClass().getSimpleName(), "[" + Misc.objString(attackedEntity) + "] killed by: [" + Misc.objString(damageComponent.source) + "]");
        }
        
        //remove missile
        damageEntity.add(new RemoveComponent());
    }
}