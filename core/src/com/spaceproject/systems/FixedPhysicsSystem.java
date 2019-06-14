package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.physics.box2d.World;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;

// based off:
// http://gafferongames.com/game-physics/fix-your-timestep/
// http://saltares.com/blog/games/fixing-your-timestep-in-libgdx-and-box2d/
public class FixedPhysicsSystem extends EntitySystem implements IRequireGameContext {
    
    private static final int velocityIterations = 6;
    private static final int positionIterations = 2;
    private static final int updatesPerSecond = 60;
    private static final float timeStep = 1 / (float)updatesPerSecond;
    private static float accumulator = 0f;
    
    private World world;
    
    private ImmutableArray<Entity> entities;
    
    @Override
    public void initContext(GameScreen gameScreen) {
        this.world = gameScreen.world;
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        Family family = Family.all(BoundsComponent.class, TransformComponent.class).get();
        entities = engine.getEntitiesFor(family);
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
            BoundsComponent physics = Mappers.bounds.get(entity);
            
            if (!physics.body.isActive()) {
                return;
            }
            
            TransformComponent transform = Mappers.transform.get(entity);
            transform.pos.set(physics.body.getPosition());
            transform.rotation = physics.body.getAngle();
        }
    }
    
    private void interpolate(float deltaTime, float accumulator) {
    
    }
    
}