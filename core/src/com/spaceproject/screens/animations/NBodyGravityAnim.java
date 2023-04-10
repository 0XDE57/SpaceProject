package com.spaceproject.screens.animations;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

public class NBodyGravityAnim extends TitleAnimation implements Disposable {
    
    
    class SimpleBody {
        
        Vector2 pos;
        Vector2 vel;
        Vector2 accel;
        float radius;
        float mass;
        
        Vector2[] tail;
        int tailIndex;
        
        public SimpleBody(float x, float y, float velX, float velY, float radius) {
            pos = new Vector2(x, y);
            vel = new Vector2(velX, velY);
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
        
        public SimpleBody(float x, float y, float radius) {
            this(x, y, 0, 0, radius);
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
    
        public void merge(SimpleBody bodyB) {
            radius += bodyB.radius;//absorb mass
            calculateMass();
            //vel.crs(bodyB.vel);//merge velocity
            //pos.add(pos.cpy().sub(bodyB.pos));//split the difference
        }
    }
    
    float timeAccumulator;
    float simulationSpeed;
    float gravity;
    int tailSize = 200;
    
    Array<SimpleBody> bodies = new Array<>();
    SimpleBody ghostBody = null;
    
    boolean isDrag = false;
    SimpleBody selectedBody = null;
    int releaseMS = 200;
    long dragTime;
    
    Stage stage;
    VisWindow window;
    VisSlider simulationSpeedSlider;
    VisSlider gravitySlider;
    
    public NBodyGravityAnim(Stage stage) {
        resetInitialBodies();
        
        this.stage = stage;
        buildWindow();
    }
    
    private void buildWindow() {
        window = new VisWindow("n-body");
        
        //https://github.com/kotcrab/vis-ui/wiki/VisValidatableTextField
        final VisTextField gravityValue = new VisTextField();
        gravityValue.setText(gravity + "");
        gravityValue.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println();
            }
        });
        gravityValue.pack();
        float maxGravity = 5;
        gravitySlider = new VisSlider(-maxGravity, maxGravity, 0.1f, false);
        gravitySlider.setValue(gravity);
        gravitySlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gravity = gravitySlider.getValue();
                gravityValue.setText(gravity + "");
            }
        });
        gravitySlider.pack();
    
    
        final VisTextField simSpeedValue = new VisTextField();
        simSpeedValue.setText(simulationSpeed + "");
        //gravityValue.addListener();
        simSpeedValue.pack();
        int maxSim = 100;
        simulationSpeedSlider = new VisSlider(-maxSim, maxSim, 0.1f, false);
        simulationSpeedSlider.setValue(simulationSpeed);
        simulationSpeedSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                simulationSpeed = simulationSpeedSlider.getValue();
                simSpeedValue.setText(simulationSpeed + "");
            }
        });
        simulationSpeedSlider.pack();
        
        final VisTextButton resetButton = new VisTextButton("reset");
        resetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                resetInitialBodies();
                gravitySlider.setValue(gravity);
                simulationSpeedSlider.setValue(simulationSpeed);
            }
        });
        
        window.add(simSpeedValue);
        window.add(simulationSpeedSlider);
        window.add().row();
        window.add(gravityValue);
        window.add(gravitySlider);
        window.add().row();
        window.add(resetButton);
        
        //radio button: no-collision, bounce, absorb
        //---
        //num bodies, total mass
        //---
        //window.add(resetbutton);
        
        window.pack();
    }
    
    private void resetInitialBodies() {
        bodies.clear();
        //classic stable figure 8
        //initial condition computed by Carles Sim ́o
        //x1=−x2=0.97000436−0.24308753i,x3=0; ~V =  ̇x3=−2  ̇x1=−2  ̇x2=−0.93240737−0.86473146i
        Vector2 x1 = new Vector2(-0.97000436f, 0.24308753f);
        x1.scl(300);//make bigger to fill screen
        Vector2 x2 = new Vector2(-x1.x, -x1.y);
        Vector2 x3 = new Vector2(0, 0); //center body
        
        Vector2 vx3 = new Vector2(0.93240737f, 0.86473146f);
        vx3.scl(4.1f);//todo: not accurate scaling: not stable
        Vector2 vx2 = new Vector2(-vx3.x / 2, -vx3.y / 2);
        Vector2 vx1 = new Vector2(vx2.x, vx2.y);
    
        //center bodies on screen
        x1.add(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
        x2.add(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
        x3.add(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
        
        float radius = 40;//somewhat magic number
        bodies.add(new SimpleBody(x1.x, x1.y, vx1.x, vx1.y, radius));
        bodies.add(new SimpleBody(x2.x, x2.y, vx2.x, vx2.y, radius));
        bodies.add(new SimpleBody(x3.x, x3.y, vx3.x, vx3.y, radius));
        
        //reset simulation parameters
        gravity = 1f;
        simulationSpeed = 15.0f;
        
        Gdx.app.log(this.getClass().getSimpleName(), "reset initial bodies. initial settings: " + gravity );
    }
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        //update simulation
        physicsStep(delta * simulationSpeed);
        timeAccumulator += delta;
        
        //reset to initial conditions
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            stage.addActor(window);
            window.setPosition(stage.getWidth(), 0);
            /*
            resetInitialBodies();
            gravitySlider.setValue(gravity);
            simulationSpeedSlider.setValue(simulationSpeed);*/
        }
        
        //mouse position
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();
    
        //add new body
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (ghostBody == null) {
                ghostBody = new SimpleBody(0, 0, 5);
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
                        Gdx.app.log(this.getClass().getSimpleName(),
                                "removed body. (" + selectedBody.mass + ")  " + selectedBody.pos + " total: " + bodies.size);
                    }
                }
    
                selectedBody = null;
            }
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
            ghostBody.pos.x = x;
            ghostBody.pos.y = y;
            ghostBody.radius += Math.sin(timeAccumulator) * 0.5f;
            
            shape.setColor(1, 1, 1, 1);
            shape.circle(ghostBody.pos.x, ghostBody.pos.y, ghostBody.radius);
        }
        
        shape.end();

    }
    
    /** integration */
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
        
        /*
        //collision, merge if touching and velocity difference is low enough
        boolean absorb;
        do {
            absorb = false;
            for (int index = 0; index < bodies.size && absorb == false; index++) {
                SimpleBody bodyA = bodies.get(index);
                for (int indexB = 0; indexB < index; indexB++) {
                    SimpleBody bodyB = bodies.get(indexB);
                    float length = bodyA.pos.cpy().sub(bodyB.pos).len();
                    float rad = bodyA.radius + bodyB.radius;
                    if (rad >= length) {
                        float dotVelocity = bodyA.vel.cpy().dot(bodyB.vel);
                        float deltaAccel = bodyA.accel.cpy().sub(bodyB.accel).len();
                        float deltaVel = bodyA.vel.cpy().sub(bodyB.vel).len();
                        float dotAccel = bodyA.accel.cpy().dot(bodyB.accel);
                        if (MathUtils.isEqual(dotVelocity, 0f, 0.1f)) {
                            Gdx.app.log("", "merge!");
                            if (bodyA.radius - bodyB.radius > 0) {
                                bodyA.merge(bodyB);
                                bodies.removeValue(bodyB, true);
                            } else {
                                bodyB.merge(bodyA);
                                bodies.removeValue(bodyA, true);
                            }
                            absorb = true;
                            break;
                        }
                    }
                }
            }
        } while (absorb);
        */
    }
    
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
    
    @Override
    public void dispose() {
        window.remove();
    }
    
}
