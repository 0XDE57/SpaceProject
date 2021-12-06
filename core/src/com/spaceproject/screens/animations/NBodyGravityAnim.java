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
        
        Vector2[] tail;
        int tailIndex;
        
        public SimpleBody(float x, float y, float radius) {
            pos = new Vector2(x, y);
            vel = new Vector2();
            accel = new Vector2();
            this.radius = radius;
            
            //basic model all bodies assume same density
            //mass = (float) (radius * radius * Math.PI);
            //float density = random 0 to 1?
            calculateMass();
            
            //init tail: pre-allocate vectors
            tail = new Vector2[tailSize];
            for (int index = 0; index < tail.length; index++) {
                tail[index] = new Vector2();
            }
            tailIndex = 0;//start at beginning
        }
        
        public void calculateMass() {
            mass = (float) (radius * radius * Math.PI);
        }
        
        public void updateTail() {
            tail[tailIndex].set(pos.x, pos.y);
            
            tailIndex++;
            if (tailIndex >= tail.length) {
                tailIndex = 0;
            }
        }
        
    }
    
    float timeAccumulator = 0;
    float simulationSpeed = 15.0f;
    float gravity = 0.5f;
    int tailSize = 200;
    
    Array<SimpleBody> bodies = new Array<>();
    SimpleBody ghostBody = null;
    
    boolean isDrag = false;
    SimpleBody selectedBody = null;
    int releaseMS = 200;
    long dragTime;
    
    public NBodyGravityAnim() {
        resetInitialBodies();
    }
    
    private void resetInitialBodies() {
        bodies.clear();
        //todo: set initial bodies to "stable" figure 8 shape
        // initial conditions = ?
        // https://www.youtube.com/watch?v=et7XvBenEo8
        // https://www.youtube.com/watch?v=otRtUiCcCh4
        // https://www.youtube.com/watch?v=8_RRZcqBEAc
        bodies.add(new SimpleBody(800, 500, 50));
        bodies.add(new SimpleBody(400, 400, 50));
        bodies.add(new SimpleBody(600, 250, 50));
    }
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        //update simulation
        physicsStep(delta * simulationSpeed);
        timeAccumulator += delta;
    
        
        //mouse position
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();
    
        //add new body
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (ghostBody == null) {
                ghostBody = new SimpleBody(0, 0, 10);
                timeAccumulator = 0;
            }
        } else {
            if (ghostBody != null) {
                ghostBody.calculateMass();//recalc mass due to radius change
                bodies.add(ghostBody);
                Gdx.app.log(this.getClass().getSimpleName(), "added body.(" + ghostBody.mass + ") total: " + bodies.size);
                ghostBody = null;
            }
        }
    
        
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            //hold-drag
            if (!isDrag) {
                for (SimpleBody body : bodies) {
                    Circle circle = new Circle(body.pos, body.radius);
                    if (circle.contains(x, y)) {
                        isDrag = true;
                        selectedBody = body;
                        dragTime = System.currentTimeMillis();
                        Gdx.app.log(this.getClass().getSimpleName(), "selected body: (" + body.mass + ")  " + body.pos);
                        break;
                    }
                }
            } else {
                Vector2 deltaMouse = selectedBody.pos.cpy().sub(x, y);
                selectedBody.vel.sub(deltaMouse.scl(deltaMouse.len() * 0.0001f * delta));
            }
        } else {
            //release: fling
            if (isDrag) {
                isDrag = false;
                
                //remove body if release was quick, and still in body
                if (System.currentTimeMillis() - dragTime <= releaseMS) {
                    //bodies.removeValue(selectedBody, true);
                    Circle circle = new Circle(selectedBody.pos, selectedBody.radius);
                    if (circle.contains(x, y)) {
                        bodies.removeValue(selectedBody, true);
                        Gdx.app.log(this.getClass().getSimpleName(), "removed body. (" + selectedBody.mass + ")  " + selectedBody.pos + " total: " + bodies.size);
                    }
                } else {
                    //fling
                    //selectedBody.vel.sub(selectedBody.pos.cpy().sub(x, y).scl(0.05f));
                }
    
                selectedBody = null;
            }
        }
        
        /*
        //remove body
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            //if not holdToggle
            if (!isDrag) {
                for (SimpleBody body : bodies) {
                    Circle circle = new Circle(body.pos, body.radius);
                    if (circle.contains(x, y)) {
                        bodies.removeValue(body, true);
                        Gdx.app.log(this.getClass().getSimpleName(), "removed body. total: " + bodies.size);
                        break;
                    }
                }
            }
        }*/
    
        //reset to initial conditions
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            resetInitialBodies();
        }
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 1);
        
        //draw bodies
        for (SimpleBody body : bodies) {
            shape.setColor(0, 0, 0, 1);
            shape.circle(body.pos.x, body.pos.y, body.radius);
        }
        
        //draw tails
        shape.setColor(1, 1, 1, 1);
        for (SimpleBody body: bodies) {
            body.updateTail();
            for (int index = 0; index < body.tail.length; index++) {
                Vector2 tail = body.tail[index];
                shape.point(tail.x, tail.y, 0);
                //shape.circle(tail.x, tail.y, 2);
            }
        }
        
        if (isDrag) {
            shape.line(x, y, selectedBody.pos.x, selectedBody.pos.y);
        }
        
        //render and update new ghost body
        if (ghostBody != null) {
            ghostBody.pos.x = Gdx.input.getX();
            ghostBody.pos.y = Gdx.graphics.getHeight() - Gdx.input.getY();
            ghostBody.radius += Math.sin(timeAccumulator) * 0.5f;
            
            shape.setColor(1, 1, 1, 1);
            shape.circle(ghostBody.pos.x, ghostBody.pos.y, ghostBody.radius);
        }
        
        shape.end();
    }
    
    /** Verlet integration */
    private void physicsStep(float delta) {
        float step = 0.5f * delta;
        
        // add velocity from momentum to position (first pass)
        for (SimpleBody body : bodies) {
            body.pos.set(body.pos.cpy().add(body.vel.x * step, body.vel.y * step));
        }
        
        // calculate force bodies act upon each other
        calculateForces();
    
        // add acceleration to velocity
        for (SimpleBody body : bodies) {
            body.vel.set(body.vel.cpy().add(body.accel.x * delta, body.accel.y * delta));
        }
    
        // add velocity from gravitational force to position (second pass)
        for (SimpleBody body : bodies) {
            body.pos.set(body.pos.cpy().add(body.vel.x * step, body.vel.y * step));
        }
    }
    
    /**
     *
     */
    private void calculateForces() {
        for (int index = 0; index < bodies.size; index++) {
            SimpleBody bodyA = bodies.get(index);
            bodyA.accel.set(0, 0);
            
            for (int indexB = 0; indexB < index; indexB++) {
                SimpleBody bodyB = bodies.get(indexB);
                
                //calculate force of attraction between bodies
                Vector2 delta = bodyA.pos.cpy().sub(bodyB.pos);
                float norm = (float) Math.sqrt(100.0 + delta.len2());
                float mag = gravity / (norm * norm * norm);
                
                //apply force
                bodyA.accel.set(bodyA.accel.cpy().sub(delta.x * (mag * bodyB.mass), delta.y * (mag * bodyB.mass)));
                bodyB.accel.set(bodyB.accel.cpy().add(delta.x * (mag * bodyA.mass), delta.y * (mag * bodyA.mass)));
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
    
    }
    
}
