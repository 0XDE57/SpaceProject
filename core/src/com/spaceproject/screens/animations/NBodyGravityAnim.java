package com.spaceproject.screens.animations;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class NBodyGravityAnim extends TitleAnimation {
    
    class SimpleBody {
        Vector2 pos;
        Vector2 vel;
        Vector2 accel;
        float radius;
        float mass;
        
        public SimpleBody(float x, float y, float radius) {
            pos = new Vector2(x, y);
            vel = new Vector2();
            accel = new Vector2();
            this.radius = radius;
            mass = (float) (radius * radius * Math.PI);
        }
    }
    
    float timeAccumulator = 0;
    float simulationSpeed = 15.0f;
    float gravity = 0.5f;
    
    Array<SimpleBody> bodies = new Array<>();
    SimpleBody newBody = null;
    
    public NBodyGravityAnim() {
        resetInitialBodies();
    }
    
    private void resetInitialBodies() {
        bodies.clear();
        //todo: set initial bodies to "stable" figure 8 shape
        bodies.add(new SimpleBody(800, 500, 50));
        bodies.add(new SimpleBody(400, 400, 50));
        bodies.add(new SimpleBody(600, 250, 50));
    }
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        //update simulation
        physicsStep(delta * simulationSpeed);
        timeAccumulator += delta;
    
        //add new body
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (newBody == null) {
                newBody = new SimpleBody(0, 0, 10);
                timeAccumulator = 0;
            }
        } else {
            if (newBody != null) {
                bodies.add(newBody);
                newBody = null;
            }
        }
    
        //remove body
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY();
            for (SimpleBody body : bodies) {
                Circle circle = new Circle(body.pos, body.radius);
                if (circle.contains(x, y)) {
                    bodies.removeValue(body, true);
                    break;
                }
            }
        }
    
        //reset to initial conditions
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            resetInitialBodies();
        }
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 1);
        
        //draw bodies
        for (SimpleBody body : bodies) {
            shape.circle(body.pos.x, body.pos.y, body.radius);
        }
        
        
        //render new body ghost
        if (newBody != null) {
            newBody.pos.x = Gdx.input.getX();
            newBody.pos.y = Gdx.graphics.getHeight() - Gdx.input.getY();
            newBody.radius += Math.sin(timeAccumulator) * 0.5f;
            
            shape.setColor(1, 1, 1, 1);
            shape.circle(newBody.pos.x, newBody.pos.y, newBody.radius);
        }
        
        shape.end();
    }
    
    /** Verlet integration */
    private void physicsStep(float delta) {
        // add velocity to position (first pass)
        for (SimpleBody body : bodies) {
            body.pos.set(body.pos.cpy().add(body.vel.x * (0.5f * delta), body.vel.y * (0.5f * delta)));
        }
        
        // calculate force bodies act upon each other
        calculateForces();
    
        // add acceleration to velocity
        for (SimpleBody body : bodies) {
            body.vel.set(body.vel.cpy().add(body.accel.x * delta, body.accel.y * delta));
        }
    
        // add velocity to position (second pass)
        for (SimpleBody body : bodies) {
            body.pos.set(body.pos.cpy().add(body.vel.x * (0.5f * delta), body.vel.y * (0.5f * delta)));
        }
    }
    
    private void calculateForces() {
        for (int index = 0; index < bodies.size; index++) {
            SimpleBody bodyA = bodies.get(index);
            bodyA.accel.set(0, 0);
            
            for (int indexB = 0; indexB < index; indexB++) {
                SimpleBody bodyB = bodies.get(indexB);
                
                //calculate force of attraction between bodies
                Vector2 d = bodyA.pos.cpy().sub(bodyB.pos);
                float norm = (float) Math.sqrt(100.0 + d.len2());
                float mag = gravity / (norm * norm * norm);
                
                //apply force
                bodyA.accel.set(bodyA.accel.cpy().sub(d.x * (mag * bodyB.mass), d.y * (mag * bodyB.mass)));
                bodyB.accel.set(bodyB.accel.cpy().add(d.x * (mag * bodyA.mass), d.y * (mag * bodyA.mass)));
            }
        }
        
    }
    
    
    @Override
    public void resize(int width, int height) {
    
    }
}
