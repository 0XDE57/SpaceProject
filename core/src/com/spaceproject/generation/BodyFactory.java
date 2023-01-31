package com.spaceproject.generation;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class BodyFactory {
    
    private static EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    
    public static Body createCircle(float x, float y, float radius, World world, Entity entity) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        body = world.createBody(bodyDef);
        
        CircleShape circle = new CircleShape();
        circle.setRadius(radius);
        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.6f; // Make it bounce a little bit
        // Create our fixture and attach it to the body
        body.createFixture(fixtureDef);
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        circle.dispose();
    
        body.setUserData(entity);
        return body;
    }

    public static Body createCircleCensor(float x, float y, float radius, World world, Entity entity) {
        Body body;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        body = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(radius);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.isSensor = true;
        body.createFixture(fixtureDef);
        circle.dispose();

        body.setUserData(entity);

        return body;
    }
    
    public static Body createDrop(float x, float y, float size, float radius, Entity entity) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        body = GameScreen.box2dWorld.createBody(bodyDef);
    
        PolygonShape box = new PolygonShape();
        box.setAsBox(size*0.5f, size*0.5f);
        FixtureDef boxFixture = new FixtureDef();
        boxFixture.shape = box;
        boxFixture.density = 0.5f;
        boxFixture.friction = 0.0f;
        boxFixture.isSensor = true;
        body.createFixture(boxFixture);
        box.dispose();
        body.setUserData(entity);
    
        return body;
    }
    
    public static Body createRect(float x, float y, float width, float height, BodyDef.BodyType bodyType, Entity entity) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = bodyType;
        bodyDef.position.set(x, y);
        body = GameScreen.box2dWorld.createBody(bodyDef);
        
        PolygonShape poly = new PolygonShape();
        poly.setAsBox(width*0.5f, height*0.5f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = poly;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.6f; // Make it bounce a little bit
        body.createFixture(fixtureDef);
        poly.dispose();
        body.setUserData(entity);
        
        return body;
    }
    
    public static Body createPlayerBody(float x, float y, Entity entity) {
        Texture texture = Mappers.texture.get(entity).texture;
        Body body = createRect(x, y,
                texture.getWidth() * engineCFG.bodyScale, texture.getHeight() * engineCFG.bodyScale,
                BodyDef.BodyType.DynamicBody, entity);
        body.setLinearDamping(10f);
        return body;
    }
    
    public static Body createShip(float x, float y, float width, float height, Entity entity, boolean inSpace) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        body = GameScreen.box2dWorld.createBody(bodyDef);
    
        //ship body
        PolygonShape box = new PolygonShape();
        box.setAsBox(width*0.5f, height*0.5f);
        // Create a fixture definition to apply our shape to
        FixtureDef boxFixture = new FixtureDef();
        boxFixture.shape = box;
        boxFixture.density = 0.5f;
        boxFixture.friction = 0.0f;
        boxFixture.restitution = 0.6f; // Make it bounce a little bit
        body.createFixture(boxFixture);
        box.dispose();
        
        //outer sensor
        float collisionRadius = 2 * width * width;
        CircleShape innerCollectSensor = new CircleShape();
        innerCollectSensor.setRadius(collisionRadius);
        FixtureDef innerCircleFixture = new FixtureDef();
        innerCircleFixture.shape = innerCollectSensor;
        innerCircleFixture.isSensor = true;
        //0x0001 is default value
        //innerCircleFixture.filter.categoryBits = 1; //inner ring: pickup
        body.createFixture(innerCircleFixture);
        innerCollectSensor.dispose();
        //inner sensor
        CircleShape outerCollectSensor = new CircleShape();
        outerCollectSensor.setRadius(4 * collisionRadius);
        FixtureDef outerCircleFixture = new FixtureDef();
        outerCircleFixture.shape = outerCollectSensor;
        outerCircleFixture.isSensor = true;
        outerCircleFixture.filter.categoryBits = 2; //outer ring: apply force, no pickup
        body.createFixture(outerCircleFixture);
        innerCollectSensor.dispose();
        
        body.setUserData(entity);
    
        if (inSpace) {
            body.setLinearDamping(0);
            body.setAngularDamping(0);
        } else {
            //todo: these are magic numbers. dunno what friction from gravity of planet should be
            body.setAngularDamping(30);
            body.setLinearDamping(45);
        }
        return body;
    }
    
    public static Body createWall(float x, float y, int width, int height, Entity entity) {
        return createRect(x, y, width, height, BodyDef.BodyType.StaticBody, entity);
    }
    
    public static Body createRect(float x, float y, int width, int height, BodyDef.BodyType bodyType, boolean isSensor, Entity entity) {
        // * 0.5f is half-width / half-height required by setAsBox()
        float scaledWidth  = engineCFG.meterPerUnit * width * 0.5f;
        float scaledHeight = engineCFG.meterPerUnit * height * 0.5f;
        
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = bodyType;
        bodyDef.position.set(x, y);
        body = GameScreen.box2dWorld.createBody(bodyDef);
        
        PolygonShape poly = new PolygonShape();
        poly.setAsBox(scaledWidth, scaledHeight);
        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = poly;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.6f; // Make it bounce a little bit
        fixtureDef.isSensor = isSensor;
        // Create our fixture and attach it to the body
        body.createFixture(fixtureDef);
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        poly.dispose();
    
        //tag user data with entity so contact listeners can access entities involved in the collision
        body.setUserData(entity);
        return body;
    }
    
    public static Body createPoly(float x, float y, float[] vertices, float angle, float density, BodyDef.BodyType bodyType,  World world, Entity entity) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = bodyType;
        bodyDef.position.set(x, y);
        bodyDef.angle = angle;
        body = world.createBody(bodyDef);
        
        PolygonShape poly = new PolygonShape();
        poly.set(vertices);
        
        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = poly;
        fixtureDef.density = density;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.6f; // Make it bounce a little bit
        // Create our fixture and attach it to the body
        body.createFixture(fixtureDef);
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        poly.dispose();
    
        //tag user data with entity so contact listeners can access entities involved in the collision
        body.setUserData(entity);
        return body;
    }
    
    public static void addShieldFixtureToBody(Body body, float size) {
        CircleShape circle = new CircleShape();
        circle.setRadius(size);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.6f;
        body.createFixture(fixtureDef);
        circle.dispose();
    }
    
}
