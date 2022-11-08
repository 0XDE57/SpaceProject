package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;

public class AsteroidShatterSystem extends EntitySystem implements EntityListener {
    
    private final DelaunayTriangulator delaunay = new DelaunayTriangulator();
    private final float minAsteroidSize = 100; //anything smaller than this will not create more
    private final float maxDriftVel = 2.0f; //drift when shatter
    private final float maxDriftAngle = 0.25f; //angular drift when shatter
    
    @Override
    public void addedToEngine(Engine engine) { }
    
    @Override
    public void update(float deltaTime) { }
    
    @Override
    public void entityAdded(Entity entity) { }
    
    @Override
    public void entityRemoved(Entity entity) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        if (asteroid == null) return;
        
        if (asteroid.doShatter && asteroid.area >= minAsteroidSize) {
            shatterAsteroid(entity, asteroid);
        }
    }
    
    private void shatterAsteroid(Entity parentAsteroid, AsteroidComponent asteroid) {
        float[] vertices = asteroid.polygon.getVertices();
        
        //debug center of mass / origin
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
        
        spawnChildAsteroid(parentAsteroid, newPoly);
    }
    
    private void spawnChildAsteroid(Entity parentAsteroid, float[] vertices) {
        /* NOTE: Box2D expects Polygons vertices are stored with a counter clockwise winding (CCW).
        We must be careful because the notion of CCW is with respect to a right-handed
        coordinate system with the z-axis pointing out of the plane. */
        
        ShortArray triangles = delaunay.computeTriangles(vertices, false);
        Gdx.app.debug(this.getClass().getSimpleName(), "shatter into " + triangles.size);
        
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
            
            float triangleQuality = GeometryUtils.triangleQuality(hull[0], hull[1], hull[2], hull[3], hull[4], hull[5]);
            //if (triangleQuality < 2.0f) {
            //todo: add new vertices to break in half
            // because the current shatter creates long ugly slivers
            //}
            
            Gdx.app.debug(this.getClass().getSimpleName(),
                    MyMath.round(hull[0],1) + ", " + MyMath.round(hull[1],1) + " | " +
                            MyMath.round(hull[2],1) + ", " + MyMath.round(hull[3],1) + " | " +
                            MyMath.round(hull[4],1) + ", " + MyMath.round(hull[5],1) +
                            " | clockwise: " + GeometryUtils.isClockwise(hull, 0, hull.length) + " | quality: " + triangleQuality);
            
            //discard duplicate points
            if ((hull[0] == hull[2] && hull[1] == hull[3]) || // p1 == p2 or
                    (hull[0] == hull[4] && hull[1] == hull[5]) || // p1 == p3 or
                    (hull[2] == hull[4] && hull[3] == hull[5])) { // p2 == p3
                
                Gdx.app.error(this.getClass().getSimpleName(), "Duplicate point! Discarding triangle");
                
                //duplicate points result in crash:
                //java: ../b2PolygonShape.cpp:158: void b2PolygonShape::Set(const b2Vec2*, int32): Assertion `false' failed.
                continue;
            }
            
            Body parentBody = Mappers.physics.get(parentAsteroid).body;
            Vector2 pos = parentBody.getPosition();
            Vector2 vel = parentBody.getLinearVelocity();
    
            //add some variation in velocity and angular so pieces drift apart
            Vector2 driftVel = MyMath.vector(MathUtils.random(0, MathUtils.PI2), maxDriftVel);
            vel.add(driftVel);
            float angularDrift = MathUtils.random(-maxDriftAngle, maxDriftAngle);
            
            Entity childAsteroid = EntityFactory.createAsteroid((long) (Math.random() * Long.MAX_VALUE),
                    pos.x, pos.y, vel.x, vel.y, parentBody.getAngle(), hull);
            Mappers.physics.get(childAsteroid).body.setAngularVelocity(parentBody.getAngularVelocity() + angularDrift);
            
            getEngine().addEntity(childAsteroid);
        }
    }
    
}