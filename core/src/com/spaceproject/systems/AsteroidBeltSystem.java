package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AsteroidBeltComponent;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.Config;
import com.spaceproject.config.DebugConfig;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class AsteroidBeltSystem extends EntitySystem {

    private ImmutableArray<Entity> asteroids;
    private ImmutableArray<Entity> spawnBelt;
    
    private final SimpleTimer lastSpawnedTimer = new SimpleTimer(1000);
    
    @Override
    public void addedToEngine(Engine engine) {
        asteroids = engine.getEntitiesFor(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        spawnBelt = engine.getEntitiesFor(Family.all(AsteroidBeltComponent.class).get());
        lastSpawnedTimer.setCanDoEvent();
    }
    
    @Override
    public void update(float deltaTime) {
        spawnAsteroidBelt();
        
        updateBeltOrbit();
    
        //debug add asteroid at mouse position
        if (GameScreen.isDebugMode && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            DebugConfig debug = SpaceProject.configManager.getConfig(DebugConfig.class);
            if (debug.spawnAsteroid) {
                Vector3 unproject = GameScreen.cam.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                spawnAsteroid(unproject.x, unproject.y, 0, 0);
                //spawnAsteroidField(unproject.x, unproject.y, 0, 80, 20, 400);
            }
        }
    }
    
    private void updateBeltOrbit() {
        //keep asteroids orbit around parent body, don't fling everything out into universe...
        for (Entity entity : asteroids) {
            AsteroidComponent asteroid = Mappers.asteroid.get(entity);
            PhysicsComponent physics = Mappers.physics.get(entity);
            if (asteroid.parentOrbitBody != null) {
                TransformComponent parentTransform = Mappers.transform.get(asteroid.parentOrbitBody);
                AsteroidBeltComponent asteroidBelt = Mappers.asteroidBelt.get(asteroid.parentOrbitBody);
                //set velocity perpendicular to parent body, (simplified 2-body model)
                float angle = MyMath.angleTo(parentTransform.pos, physics.body.getPosition()) + (asteroidBelt.clockwise ? -MathUtils.HALF_PI : MathUtils.HALF_PI);
                physics.body.setLinearVelocity(MyMath.vector(angle, asteroidBelt.velocity));
            } else {
                //todo: gravity pull into belt if close enough
                
                // re-entry?
                for (Entity parentEntity : spawnBelt) {
                    AsteroidBeltComponent asteroidBelt = Mappers.asteroidBelt.get(parentEntity);
                    TransformComponent parentTransform = Mappers.transform.get(parentEntity);
                    
                    //todo: if close enough: slowly pull into stream of asteroid, match velocity and angle
                    float dist = parentTransform.pos.dst(physics.body.getPosition());
                    if (dist > asteroidBelt.radius - (asteroidBelt.bandWidth/2) && dist < asteroidBelt.radius + (asteroidBelt.bandWidth/2)) {
                        float targetAngle = MyMath.angleTo(parentTransform.pos, physics.body.getPosition()) + (asteroidBelt.clockwise ? -MathUtils.HALF_PI : MathUtils.HALF_PI);
                        double angleDeltaThreshold = Math.PI / 6;
                        boolean meetsAngleThreshold = Math.abs(physics.body.getLinearVelocity().angleRad() - targetAngle) < angleDeltaThreshold;
                        
                        float velDeltaThreshold = 5f;
                        boolean meetsVelThreshold = Math.abs(physics.body.getLinearVelocity().len() - asteroidBelt.velocity) < velDeltaThreshold;
    
                        //todo: if should merge, begin merge
                        if (meetsAngleThreshold /*&& meetsVelThreshold*/) {
                            //asteroid.parentOrbitBody = parentEntity;
                            //Gdx.app.debug(this.getClass().getSimpleName(), "ASTEROID re-entry into orbit");
                            break; // no point looking at other disks once met
                        }
                    }
                }
            }
        }
    }
    
    private Entity spawnAsteroid(float x, float y, float velX, float velY) {
        int size = MathUtils.random(14, 120);
        long seed = MyMath.getSeed(x, y);
        Entity asteroid = EntityBuilder.createAsteroid(seed, x, y, velX, velY, size);
        getEngine().addEntity(asteroid);
        return asteroid;
    }
    
    private void spawnAsteroidBelt() {
        for (Entity parentEntity : spawnBelt) {
            AsteroidBeltComponent disk = Mappers.asteroidBelt.get(parentEntity);
            if (disk.spawned <= disk.maxSpawn) {
                //todo, should bias towards middle and taper off edges
                // alternatively could be a 1D noise from inner to outer with different concentrations?
                float bandwidthOffset = MathUtils.random(-disk.bandWidth /2, disk.bandWidth /2);
                float angle = MathUtils.random(MathUtils.PI2);
                Vector2 pos = Mappers.transform.get(parentEntity).pos.cpy();
                pos.add(MyMath.vector(angle, disk.radius + bandwidthOffset));
                
                Entity newAsteroid = spawnAsteroid(pos.x, pos.y, 0, 0);
                AsteroidComponent ast = Mappers.asteroid.get(newAsteroid);
                ast.parentOrbitBody = parentEntity;
                
                disk.spawned++;
            }
        }
    }
    
    private void spawnAsteroidField(float x, float y, float angle, float velocity, int clusterSize, float range) {
        Vector2 vel = MyMath.vector(angle, velocity);
        for (int i = 0; i < clusterSize; i++) {
            float newX = MathUtils.random(x - range, x + range);
            float newY = MathUtils.random(y - range, y + range);
            spawnAsteroid(newX, newY, vel.x, vel.y);
        }
        Gdx.app.log(this.getClass().getSimpleName(), "spawn field: " + clusterSize);
    }
    
}
