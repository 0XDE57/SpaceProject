package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.physics.box2d.World;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

// based off:
// http://gafferongames.com/game-physics/fix-your-timestep/
// http://saltares.com/blog/games/fixing-your-timestep-in-libgdx-and-box2d/
public class Box2DPhysicsSystem extends EntitySystem {
    
    private static final EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private final int velocityIterations = engineCFG.physicsVelocityIterations;
    private final int positionIterations = engineCFG.physicsPositionIterations;
    private final float timeStep = 1 / (float) engineCFG.physicsStepPerFrame;
    private float accumulator = 0f;
    
    private World world;
    private Box2DContactListener damageContactListener;

    private ImmutableArray<Entity> entities;
    
    @Override
    public void addedToEngine(Engine engine) {
        Family family = Family.all(PhysicsComponent.class, TransformComponent.class).get();
        entities = engine.getEntitiesFor(family);
    
        world = GameScreen.box2dWorld;
        damageContactListener = new Box2DContactListener(engine);
        world.setContactListener(damageContactListener);
    }
    
    @Override
    public void update(float deltaTime) {
        accumulator += deltaTime;
        while (accumulator >= timeStep) {
            world.step(timeStep, velocityIterations, positionIterations);
            accumulator -= timeStep;

            damageContactListener.updateActiveContacts(world, deltaTime);
            
            updateTransform();
        }
        
        //interpolate(deltaTime, accumulator);
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
    
    public static int getVelocityLimit() {
        //Box2D uses MKS (meters, kilograms, and seconds) units and radians for angles
        //movement limit = 2 * units per step
        //eg step of 60: 60 * 2 = 120,  max velocity = 120
        return 2 * engineCFG.physicsStepPerFrame;
    }
    
    public static int getVelocityLimit2() {
        return getVelocityLimit() * getVelocityLimit();
    }
    
}
