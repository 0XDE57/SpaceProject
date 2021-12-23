package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.CircumstellarDiscComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
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
    //      direction: which way headed
    // - asteroid source c: rogue rock "odd ball"
    //      just a single rock of random size maybe larger than usual, going in a random direction
    // - if any asteroid is too far from player, unload
    
    private ImmutableArray<Entity> asteroids;
    private ImmutableArray<Entity> spawnDisk;
    SimpleTimer lastSpawnedTimer = new SimpleTimer(1000);
    int maxSpawn = 100;
    
    ShortArray triangles;
    DelaunayTriangulator delaunay = new DelaunayTriangulator();
    
    @Override
    public void addedToEngine(Engine engine) {
        asteroids = engine.getEntitiesFor(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        spawnDisk = engine.getEntitiesFor(Family.all(CircumstellarDiscComponent.class).get());
        lastSpawnedTimer.setCanDoEvent();
    }
    
    @Override
    public void update(float deltaTime) {
        spawnAsteroidDisk();
        
        if (asteroids.size() < maxSpawn) {
            if (lastSpawnedTimer.canDoEvent()) {
                //spawnAsteroidField(-1000, -1000);
                //lastSpawnedTimer.reset();
            }
        }
        
        
        
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            Vector3 projected = GameScreen.cam.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            spawnAsteroid(projected.x, projected.y, 0, 0);
        }
        
        
        //todo: orbit asteroids around parent body, currently just flings everything out into universe...
        //for (Entity parentEntity : spawnDisk) {
        /*
        for (Entity entity : asteroids) {
            AsteroidComponent asteroid = Mappers.asteroid.get(entity);
            if (asteroid.type == AsteroidComponent.Type.orbitLocked) {
                PhysicsComponent physics = Mappers.physics.get(entity);
                physics.body.g
                //physics.body.setLinearVelocity();
            }
        }*/
    }
    
    private void spawnAsteroidDisk() {
        for (Entity parentEntity : spawnDisk) {
            CircumstellarDiscComponent disk = Mappers.circumstellar.get(parentEntity);
            if (asteroids.size() <= maxSpawn) {
                //if (lastSpawnedTimer.tryEvent()) {
                Vector2 pos = Mappers.transform.get(parentEntity).pos.cpy();
                float bandwidthOffset = MathUtils.random(-disk.width/2, disk.width/2);//todo, should bias towards middle and taper off edges
                //alternatively could be a 1D noise from inner to outer with different concentrations?
                float d = MathUtils.random(MathUtils.PI2);
                pos.add(MyMath.vector(d, disk.radius + bandwidthOffset));
                //todo: apply gravity to keep them rotating around star
                // problem: we want asteroids to spawn in rings and stay in rings generally, but allow player to shoot them out of belt.
                // I can't let the bodies float around freely with a simple n body sim as they are not stable.
                // the system will just tear itself apart. even if a "stable" orbit is found it will become unstable as the player starts interacting with it
                // currently this is why planets are locked in an elliptical orbit to sort of fake gravity.
                // do I lock the asteroids in orbit similar to the planets? maybe interpolate to where they "should" be, pulling them back into the ring
                // maybe once hit or disturbed, loses orbit component and floats freely?
                // basically it comes down to how "arcadey" vs "simulator" does it need to be fun?
                // currently there is no friction and we have conservation of momentum
                
                // plan of attack: i think i will start off with a lie, lock them in orbit when spawn.
                // once disturbed or interacted with by an outside force, unlock the orbit and let gravity take over
                // allow belt to "chain react" collapse / disperse and see what that plays like
                Vector2 velocity = MyMath.vector((float) (d + Math.PI/2), 20);
                spawnAsteroid(pos.x, pos.y, velocity.x, velocity.y);
                //}
            } else {
                lastSpawnedTimer.reset();
            }
        }
    }
    
    private void spawnAsteroidField(float x, float y) {
        float angle = 45 * MathUtils.degreesToRadians;// MathUtils.random(MathUtils.PI2);
        Vector2 vel = MyMath.vector(angle, 70 /*MathUtils.random(1, 50)*/);
        float range = 800.0f;
        int clusterSize = 20;//MathUtils.random(1, 20);
        for (int i = 0; i < clusterSize; i++) {
            float newX = MathUtils.random(x - range, x + range);
            float newY = MathUtils.random(y - range, y + range);
            spawnAsteroid(newX, newY, vel.x, vel.y);
        }
        Gdx.app.log(this.getClass().getSimpleName(), "spawn field: " + clusterSize);
    }
    
    private void spawnAsteroid(float x, float y, float velX, float velY) {
        int size = MathUtils.random(14, 120);
        long seed = MyMath.getSeed(x, y);
        Entity asteroid = EntityFactory.createAsteroid(seed, x, y, velX, velY, size);
        getEngine().addEntity(asteroid);
        //Gdx.app.debug(this.getClass().getSimpleName(), "spawned: " + x + ", " + y);
    }
    
    @Override
    public void entityAdded(Entity entity) { }
    
    @Override
    public void entityRemoved(Entity entity) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        if (asteroid != null && asteroid.doShatter) {
            spawnChildren(entity, asteroid);
        }
    }
    
    public void spawnChildren(Entity parentAsteroid, AsteroidComponent asteroid) {
        float minAsteroidSize = 100; //anything smaller than this will not create more
        if (asteroid.area >= minAsteroidSize) {
            
            float[] vertices = asteroid.polygon.getVertices();
            
            Vector2 center = new Vector2();
            GeometryUtils.polygonCentroid(vertices, 0, vertices.length, center);
            if (!asteroid.centerOfMass.epsilonEquals(center)) {
                Gdx.app.debug(this.getClass().getSimpleName(), "WARNING: polygonCentroid disagreement");
            }
            
            //create new polygons from vertices + center point to "sub shatter" into smaller polygon shards
            int length = vertices.length;
            float[] newPoly = new float[length + 2];
            System.arraycopy(vertices, 0, newPoly, 0, vertices.length);
            //add new point in center of triangle
            newPoly[length] = center.x;
            newPoly[length + 1] = center.y;

            triangles = delaunay.computeTriangles(newPoly, false);
            subShatter(parentAsteroid, newPoly);
        }
    }
    
    private void subShatter(Entity parentAsteroid, float[] vertices) {
        /* NOTE: Box2D expects Polygons vertices are stored with a counter clockwise winding (CCW).
        We must be careful because the notion of CCW is with respect to a right-handed
        coordinate system with the z-axis pointing out of the plane. */
        
        //create cells for each triangle
        for (int index = 0; index < triangles.size; index += 3) {
            int p1 = triangles.get(index) * 2;
            int p2 = triangles.get(index + 1) * 2;
            int p3 = triangles.get(index + 2) * 2;
            float[] hull = new float[] {
                    vertices[p1], vertices[p1 + 1], // xy: 0, 1
                    vertices[p2], vertices[p2 + 1], // xy: 2, 3
                    vertices[p3], vertices[p3 + 1]  // xy: 4, 5
            };

            Gdx.app.debug(this.getClass().getSimpleName(), "Clockwise: " + GeometryUtils.isClockwise(hull, 0, hull.length)
                    + " | quality: " + GeometryUtils.triangleQuality(hull[0], hull[1], hull[2], hull[3], hull[4], hull[5]));
            Gdx.app.debug(this.getClass().getSimpleName(),
                    MyMath.round(hull[0],1) + ", " + MyMath.round(hull[1],1) + " | " +
                            MyMath.round(hull[2],1) + ", " + MyMath.round(hull[3],1) + " | " +
                            MyMath.round(hull[4],1) + ", " + MyMath.round(hull[5],1));
            
            //discard duplicate points
            if ((hull[0] == hull[2] && hull[1] == hull[3]) || // p1 == p2 or
                (hull[0] == hull[4] && hull[1] == hull[5]) || // p1 == p3 or
                (hull[2] == hull[4] && hull[3] == hull[5])) { // p2 == p3
                
                Gdx.app.error(this.getClass().getSimpleName(), "Duplicate point! Discarding triangle");
                
                //duplicate points result in crash:
                //java: ../b2PolygonShape.cpp:158: void b2PolygonShape::Set(const b2Vec2*, int32): Assertion `false' failed.
                continue;
            }
    
            //todo: broken rotation when shattering some polygons rotate and jump instead of simply separating in place
            /* todo: fix triangle origin to be center
            //1. find minX minY
            //2. shift vertices to be centered around 0,0 relatively
            Vector2 center = new Vector2();
            GeometryUtils.polygonCentroid(hull, 0, hull.length, center);
            center.add(minX, minY);
            for (int i = 0; i < hull.length; i += 2) {
                hull[i] -= center.x;
                hull[i + 1] -= center.y;
            }
            //3. push by center offset so vertices are correct position relative to parent
            Vector2 newPos = parentPos.copy().add(center);
            */
            
            Body body = Mappers.physics.get(parentAsteroid).body;
            Vector2 vel = body.getLinearVelocity();
            Vector2 pos = body.getPosition();
            Entity childAsteroid = EntityFactory.createAsteroid((long) (Math.random() * Long.MAX_VALUE),
                    pos.x, pos.y, vel.x, vel.y, body.getAngle(), hull);
            Mappers.physics.get(childAsteroid).body.setAngularVelocity(body.getAngularVelocity());
            
            getEngine().addEntity(childAsteroid);
        }
    }
    
}
