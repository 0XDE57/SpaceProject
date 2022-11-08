package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.math.MyMath;

public class OrbitAnim extends TitleAnimation {
    
    Vector2 centerScreen = new Vector2();
    Array<Body> bodies;
    float angleSyncEpsilon = 0.1f;
    
    //todo: test render resonance ghost, leave marker when planets aligned
    
    public OrbitAnim() {
        bodies = new Array<>();
    
        bodies.addAll(orbitalResonance());
        //addRandomSystem();
        
        //bodies.addAll(solarSystem());
    }
    
    private void addRandomSystem() {
        //star with random planets
        Body star = new Body(null, 0);
        bodies.add(star);
        
        for (int i = 0; i < 5; i++) bodies.add(new Body(star, 100 + (i * 75)));
    }
    
    private Array<Body> orbitalResonance() {
        Array<Body> resonating = new Array<>();
        
        //star
        Body parent = new Body(null, 0);
        resonating.add(parent);
        
        int distance = 100;
        float angle = 0;
        float rotSpeed = 0.5f;
        
        int num = 5;
        for (int i = 0; i < num; i++) {
            Body planet = new Body(parent, distance * (i+1), angle, rotSpeed*(i+1));
            resonating.add(planet);
        }
        
        return resonating;
    }
    
    private Array<Body> solarSystem() {
        Array<Body> bodies = new Array<>();
        
        //Sun
        Body sun = new Body(null, 0);
        
        float scale = 10;// 1/10th or ten times smaller
        float scaledDistance = 1 / scale;
        //Mercury
        //0.39 AU, 36 million miles/57.9 million km
        Body mercury = new Body(sun, 10);
        bodies.add(mercury);
        
        //todo:
        //Venus
        //Earth
        //Mars
        //Jupiter
        //Saturn
        //Uranus
        //Neptune
        //Pluto
        
        return bodies;
    }
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        for (Body orbit : bodies) {
            orbit.update(delta);
        }
        
        //interaction: clicking aligns angle
        if (Gdx.input.isTouched()) {
            for (Body orbit : bodies) {
                centerScreen.set(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
                float angleToTouch = MyMath.angleTo(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), centerScreen.x, centerScreen.y);
                orbit.angleRadians = angleToTouch;
                //todo: drag to shift back and forth through phase
                //save start position, phase shift = delta between start x and current x
            }
        }
        
        //render orbit rings lines
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < bodies.size; i++) {
            Body body = bodies.get(i);
            //skip render star orbit (until nested orbit, then this is a bad check)
            if (body.parent == null) continue;
    
            shape.setColor(Color.BLACK);
        
            //check if orbits synchronize (angle is equal to any other angle with given epsilon)
            //highlight both, and connect
            for (int j = 0; j < bodies.size; j++) {
                //skip check self
                if (i == j) continue;
    
                Body other = bodies.get(j);
                
                //don't process star angle
                if (other.parent == null) continue;
                
                if (MathUtils.isEqual(body.angleRadians, other.angleRadians, angleSyncEpsilon)) {
                    shape.setColor(Color.WHITE);
                    shape.line(body.pos, other.pos);
                    break;//exit for loop no point continue check
                }
            }
            
            shape.circle(body.parent.pos.x, body.parent.pos.y, body.distance);//, (int) body.distance / 4);
        }
        shape.end();
        
        //render solid bodies
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (Body orbit : bodies) {
            orbit.render(shape);
        }
        shape.end();
    }
    
    @Override
    public void resize(int width, int height) {
    }
    
    private class Body {
        private Body parent;
        private Vector2 pos;
        private float angleRadians, rotSpeed;
        private float distance;
        private int size;
    
        public Body(Body parent, float distance) {
            this(parent, distance, MathUtils.random(MathUtils.PI2), MathUtils.random(0.15f, 0.8f));
        }
        
        public Body(Body parent, float distance, float angle, float rotSpeed) {
            this.parent = parent;
            this.distance = distance;
            this.angleRadians = angle;// MathUtils.random(MathUtils.PI2);
            this.rotSpeed = rotSpeed;// MathUtils.random(0.15f, 0.8f);
            
            //randomize rotation direction
            //if (MathUtils.randomBoolean())
            //this.rotSpeed = -rotSpeed;
            
            size = MathUtils.random(5, 10);
            if (parent == null) {
                size *= 3;
            }
            
            if (parent != null)
                pos = new Vector2();//MyMath.vector(angle, distance).cpy().add(parent.pos);
            else
                pos = new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        }
    
        public void update(float delta) {
            angleRadians += rotSpeed * delta;
            
            //clamp between -180 and 180 degrees
            //why? to avoid wrapping values eg: radians >= 360 != 0
            //eg: degrees = 720 (spin 360 2 times) we are left with 720 != 0 even tho its the same angle!
            //  if (degrees == 0) returns false!
            while (angleRadians < -180 * MathUtils.degRad) angleRadians += 360 * MathUtils.degRad;
            while (angleRadians >  180 * MathUtils.degRad) angleRadians -= 360 * MathUtils.degRad;
            //modulus?
            
            if (parent != null) {
                pos = MyMath.vector(angleRadians, distance).cpy().add(parent.pos);
            } else {
                pos.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
            }
        }
        
        public void render(ShapeRenderer shape) {
            if (parent != null) {
                shape.setColor(Color.BLACK);
                shape.circle(pos.x, pos.y, size);
            } else {
                shape.setColor(Color.BLACK);
                shape.circle(pos.x, pos.y, size);
                shape.setColor(Color.WHITE);
                shape.circle(pos.x, pos.y, size-1);
            }
        }
    }
    
}
