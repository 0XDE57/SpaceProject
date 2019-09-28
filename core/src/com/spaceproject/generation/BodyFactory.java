package com.spaceproject.generation;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.spaceproject.screens.GameScreen;

public class BodyFactory {
    
    public static Body createCircle(float x, float y, float radius) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        body = GameScreen.box2dWorld.createBody(bodyDef);
        
        CircleShape circle = new CircleShape();
        circle.setRadius(radius);
        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.4f;
        fixtureDef.restitution = 0.6f; // Make it bounce a little bit
        // Create our fixture and attach it to the body
        body.createFixture(fixtureDef);
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        circle.dispose();
        return body;
    }
    
    public static Body createRect(float x, float y, float width, float height) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        body = GameScreen.box2dWorld.createBody(bodyDef);
        
        PolygonShape poly = new PolygonShape();
        poly.setAsBox(width/2, height/2);
        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = poly;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.6f; // Make it bounce a little bit
        // Create our fixture and attach it to the body
        body.createFixture(fixtureDef);
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        poly.dispose();
        return body;
    }
    
    public static Body createPlayerBody(float x, float y, Entity entity) {
        Body body = createRect(x, y, 0.4f, 0.4f);
        body.setLinearDamping(10f);
        body.setUserData(entity);
        return body;
    }
    
    public static Body createShip(float x, float y, float width, float height, Entity entity) {
        Body body = createRect(x, y, width, height);
        body.setUserData(entity);
        return body;
    }
}
