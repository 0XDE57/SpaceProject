package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.CircumstellarDiscComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class AsteroidSpawner extends EntitySystem implements EntityListener {
    //todo:
    // - asteroid source A: belt / circumstellar disc
    //   a ring of random destructible asteroids around a planet
    //      belt radius: range from source body
    //      belt width: how wide bodies spawn from concentration of bodies at belt
    //      belt density: how many asteroids to populate belt with
    //      direction: which way they orbit around body
    // - asteroid source B: "field" / cluster
    //   random group out in the depths of space
    //      pocket size: how many to spawn in group
    //      direction:
    // - asteroid source c: rogue rock "odd ball"
    //      just a single rock of random size, going in a random direction
    // - if any asteroid is too far from player, unload
    
    private ImmutableArray<Entity> asteroids;
    private ImmutableArray<Entity> spawnDisk;
    SimpleTimer lastSpawnedTimer = new SimpleTimer(300);
    int maxSpawn = 100;
    
    @Override
    public void addedToEngine(Engine engine) {
        asteroids = engine.getEntitiesFor(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        spawnDisk = engine.getEntitiesFor(Family.all(CircumstellarDiscComponent.class).get());
        //stars = engine.getEntitiesFor(Family.all(StarComponent.class, TransformComponent.class).get());
        lastSpawnedTimer.setCanDoEvent();
    }
    
    @Override
    public void update(float deltaTime) {
        //super.update(deltaTime);
        
        for (Entity parentEntity : spawnDisk) {
            CircumstellarDiscComponent disk = Mappers.circumstellar.get(parentEntity);
            if (asteroids.size() <= maxSpawn) {
                //if (lastSpawnedTimer.tryEvent()) {
                Vector2 pos = Mappers.transform.get(parentEntity).pos.cpy();
                float offset = MathUtils.random(-disk.width/2, disk.width/2);
                float d = MathUtils.random(MathUtils.PI2);
                pos.add(MyMath.vector(d, disk.radius + offset));
                //todo: apply gravity to keep them rotating around star
                Vector2 velocity = MyMath.vector((float) (d + Math.PI/2), 20);
                spawnAsteroid(pos.x, pos.y, velocity.x, velocity.y);
                //}
            } else {
                lastSpawnedTimer.reset();
            }
        }
        
        /*
        if (asteroids.size() <= maxSpawn) {
            if (lastSpawnedTimer.tryEvent()) {
                spawnAsteroid();
            }
        } else {
            lastSpawnedTimer.reset();
        }*/
        
    }
    
    private void spawnAsteroid() {
        float range = 500.0f;
        //spawnAsteroid(MathUtils.random(-range, range), MathUtils.random(-range, range));
    }
    
    private void spawnAsteroid(float x, float y, float velX, float velY) {
        int size = MathUtils.random(14, 80);
        long seed = MyMath.getSeed(x, y);
        Entity asteroid = EntityFactory.createAsteroid(seed, x, y, velX, velY, size);
        getEngine().addEntity(asteroid);
        Gdx.app.debug(this.getClass().getSimpleName(), "spawned: " + x + ", " + y);
    }
    
    @Override
    public void entityAdded(Entity entity) {
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        if (asteroid != null) {
            float minAsteroidSize = 14; //anything smaller than this will not create
            if (asteroid.size >= minAsteroidSize) {
                //todo: size = previousSize / numChildren?
                
                TransformComponent parentTransform = Mappers.transform.get(entity);
                float x = parentTransform.pos.x;
                float y = parentTransform.pos.y;
                float size = asteroid.size * 0.4f;
                long seed = MyMath.getSeed(x, y);
                float spread = 45;
                Vector2 parentVelocity = entity.getComponent(PhysicsComponent.class).body.getLinearVelocity().rotateDeg(spread);
                Vector2 vel2 = parentVelocity.cpy().rotateDeg(-spread*2);
                
                //create two children
                Entity childAsteroidA = EntityFactory.createAsteroid(seed, x, y, parentVelocity.x, parentVelocity.y, size);
                Entity childAsteroidB = EntityFactory.createAsteroid(seed+1, x, y, vel2.x, vel2.y, size);
                getEngine().addEntity(childAsteroidA);
                getEngine().addEntity(childAsteroidB);
            }
        }
    }
    
}
