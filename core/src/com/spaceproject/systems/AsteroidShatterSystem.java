package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.ItemDropComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.TextureGenerator;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class AsteroidShatterSystem extends EntitySystem implements EntityListener {
    
    private final DelaunayTriangulator delaunay = new DelaunayTriangulator();
    private final float minAsteroidSize = 100; //anything smaller than this will not create more
    private final float maxDriftAngle = 0.25f; //angular drift when shatter
    
    @Override
    public void addedToEngine(Engine engine) {
        engine.addEntityListener(this);
    }
    
    @Override
    public void removedFromEngine(Engine engine) {
        engine.removeEntityListener(this);
    }
    
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
        } else {
            dropResource(entity, asteroid);
        }
    }
    
    private void shatterAsteroid(Entity parentAsteroid, AsteroidComponent asteroid) {
        //create new polygons from vertices + center point to "sub shatter" into smaller polygon shards
        float[] vertices = asteroid.polygon.getVertices();
        int length = vertices.length;
        float[] newPoly = new float[length + 2];
        System.arraycopy(vertices, 0, newPoly, 0, vertices.length);
        
        Vector2 center = new Vector2();
        GeometryUtils.polygonCentroid(vertices, 0, vertices.length, center);
        newPoly[length] = center.x;
        newPoly[length + 1] = center.y;
        
        spawnChildAsteroid(parentAsteroid, newPoly);
    }
    
    private void spawnChildAsteroid(Entity parentAsteroid, float[] vertices) {
        /* todo: re shatter issues; if we turn on b2d debug we can see the velocity is not the origin of child shards
        NOTE: Box2D expects Polygons vertices are stored with a counter clockwise winding (CCW).
        We must be careful because the notion of CCW is with respect to a right-handed
        coordinate system with the z-axis pointing out of the plane.
       
        NOTE: The body definition gives you the chance to initialize the position of the body on creation.
        This has far better performance than creating the body at the world origin and then moving the body.
        Caution: Do not create a body at the origin and then move it. If you create several bodies at the origin, then performance will suffer.
        A body has two main points of interest. The first point is the body's origin. Fixtures and joints are attached relative to the body's origin.
        The second point of interest is the center of mass.
        The center of mass is determined from mass distribution of the attached shapes or is explicitly set with b2MassData.
        Much of Box2D's internal computations use the center of mass position. For example b2Body stores the linear velocity for the center of mass.
        When you are building the body definition, you may not know where the center of mass is located.
        Therefore you specify the position of the body's origin.
        You may also specify the body's angle in radians, which is not affected by the position of the center of mass.
        If you later change the mass properties of the body, then the center of mass may move on the body,
        but the origin position does not change and the attached shapes and joints do not move.
        
        asteroid.centerOfMass == B2D::body->GetLocalCenter()
        
        Vector2 center = new Vector2();
        Polygon poly = new Polygon();
        poly.getCentroid(center); -> transformed vertices -> GeometryUtils.polygonCentroid()
        
        GeometryUtils.polygonCentroid()
        
        API Addition: Polygon methods setVertex, getVertex, getVertexCount, getCentroid.
        API Addition: GeometryUtils,polygons isCCW, ensureClockwise, reverseVertices
        https://libgdx.com/news/2022/05/gdx-1-11
        
        computeTriangles():
            - Duplicate points will result in undefined behavior. sorted – If false, the points will be sorted by the x coordinate,
              which is required by the triangulation algorithm. If sorting is done the input array is not modified,
              the returned indices are for the input array, and count*2 additional working memory is needed.
            - Returns: triples of indices into the points that describe the triangles in clockwise order.
        */
        
        ShortArray triangleIndices = delaunay.computeTriangles(vertices, false);
        Gdx.app.debug(getClass().getSimpleName(), "shatter into " + triangleIndices.size / 3);
        
        //create cells for each triangle
        for (int index = 0; index < triangleIndices.size; index += 3) {
            int p1 = triangleIndices.get(index) * 2;
            int p2 = triangleIndices.get(index + 1) * 2;
            int p3 = triangleIndices.get(index + 2) * 2;
            float[] hull = new float[] {
                    vertices[p1], vertices[p1 + 1], // xy: 0, 1
                    vertices[p2], vertices[p2 + 1], // xy: 2, 3
                    vertices[p3], vertices[p3 + 1]  // xy: 4, 5
            };
            
            //discard duplicate points
            if ((hull[0] == hull[2] && hull[1] == hull[3]) || // p1 == p2 or
                    (hull[0] == hull[4] && hull[1] == hull[5]) || // p1 == p3 or
                    (hull[2] == hull[4] && hull[3] == hull[5])) { // p2 == p3
                Gdx.app.error(getClass().getSimpleName(), "Duplicate point! Discarding triangle");
                //duplicate points result in crash:
                //../b2PolygonShape.cpp:158: void b2PolygonShape::Set(const b2Vec2*, int32): Assertion `false' failed.
                continue;
            }
    
            GeometryUtils.ensureCCW(hull);
    
            //shift vertices to be centered
            Vector2 center = new Vector2();
            GeometryUtils.triangleCentroid(
                    hull[0], hull[1],
                    hull[2], hull[3],
                    hull[4], hull[5],
                    center);
            for (int j = 0; j < hull.length; j += 2) {
                //hull[j] -= center.x;
                //hull[j + 1] -= center.y;
            }
            
            long seed = (long) (Math.random() * Long.MAX_VALUE);
            Body parentBody = Mappers.physics.get(parentAsteroid).body;
            //todo: rotate center relative to parent by angle
            Vector2 pos = parentBody.getPosition().cpy();//.mulAdd(center.rotateAroundRad(parentBody.getPosition(), parentBody.getAngle()), 0.1f);
            //center.rotateAroundRad(parentBody.getPosition(), parentBody.getAngle());
            //Vector2 pos = parentBody.getPosition().cpy().add(center);
            Vector2 vel = parentBody.getLinearVelocity();
            Entity childAsteroid = EntityFactory.createAsteroid(seed, pos.x, pos.y, vel.x, vel.y, parentBody.getAngle(), hull);
            //float angularDrift = MathUtils.random(-maxDriftAngle, maxDriftAngle);
            //Mappers.physics.get(childAsteroid).body.setAngularVelocity(parentBody.getAngularVelocity() + angularDrift);
            getEngine().addEntity(childAsteroid);
        }
    }
    
    public void breakPoly(Polygon p){
        DelaunayTriangulator d = new DelaunayTriangulator();
        ShortArray s = d.computeTriangles(p.getVertices(),false);
        Array<Polygon> asteroids = new Array<>();
        for(int i = 0; i < s.size-2; i+=3){
            Polygon p1 = new Polygon();
            Vector2 temp = new Vector2();
            p1.setVertices(new float[]{
                    p.getVertex(s.get(i), temp).x, p.getVertex(s.get(i), temp).y,
                    p.getVertex(s.get(i+1), temp).x, p.getVertex(s.get(i+1), temp).y,
                    p.getVertex(s.get(i+2), temp).x, p.getVertex(s.get(i+2), temp).y,
            });
            Vector2 t1 = p1.getCentroid(new Vector2());
            Vector2 t2 = p.getCentroid(new Vector2());
            Vector2 move = new Vector2((float) -Math.sqrt(t1.dst(t2)),0);
            float r = MathUtils.atan2(t2.y - t1.y, t2.x - t1.x);
            move.rotateRad(r);
            p1.setPosition(move.x,move.y);
            asteroids.add(p1);
        }
        //return asteroids;
    }
    
    private void dropResource(Entity entity, AsteroidComponent asteroid) {
        Entity drop = new Entity();
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureGenerator.createTile(asteroid.color);
        //todo: rng texture shape between circle, triangle, square
        //texture.texture = TextureFactory.createCircle(asteroid.color);
        texture.scale = 1f;
    
        TransformComponent transform = new TransformComponent();
        PhysicsComponent sourcePhysics = Mappers.physics.get(entity);
        Vector2 pos = transform.pos.set(sourcePhysics.body.getPosition());
        
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyBuilder.createDrop(pos.x, pos.y, 2, drop);
        float spin = -0.2f;
        physics.body.applyAngularImpulse(MathUtils.random(-spin, spin), true);
        //todo: give momentum? either copy asteroids velocity or send in players direction
        
        //expire time (self destruct)
        ExpireComponent expire = new ExpireComponent();
        expire.timer = new SimpleTimer(60 * 1000, true);
        
        drop.add(texture);
        drop.add(transform);
        drop.add(physics);
        drop.add(new ItemDropComponent());
        drop.add(expire);
        getEngine().addEntity(drop);
    }
    
}
