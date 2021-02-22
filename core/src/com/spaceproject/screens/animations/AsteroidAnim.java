package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.spaceproject.utility.CustomShapeRenderer;
import com.spaceproject.utility.MyMath;

import java.util.Iterator;

public class AsteroidAnim extends TitleAnimation {
    
    Vector2 bullet = null;
    float bulletAngle;
    float bulletVelocity = 300;
    Array<Asteroid> asteroids = new Array<Asteroid>();
    
    CustomShapeRenderer customShapeRenderer;
    
    public AsteroidAnim() {
        asteroids.add(new Asteroid(new Vector2(Gdx.graphics.getWidth() * MathUtils.random(), Gdx.graphics.getHeight() * MathUtils.random()), 200, 0, 0));
        customShapeRenderer = new CustomShapeRenderer(ShapeRenderer.ShapeType.Filled, new ShapeRenderer().getRenderer());
    }
    
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        Vector2 centerScreen = new Vector2(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
        Vector2 mousePos = new Vector2(Gdx.input.getX(),Gdx.graphics.getHeight()-Gdx.input.getY());
        //Vector3 proj = new Vector3(mousePos, 0);
        //cam.unproject(proj);
        float mouseAngle = MyMath.angleTo(mousePos.x, mousePos.y, centerScreen.x, centerScreen.y);
        //mouseAngle = MyMath.angle2(mousePos, centerScreen);
        
        
        if (bullet == null) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                bullet = centerScreen.cpy();
                bulletAngle = mouseAngle;
            }
        } else {
            if (bullet.x <= 0 || bullet.y <= 0 || bullet.x >= Gdx.graphics.getWidth() || bullet.y >= Gdx.graphics.getHeight()) {
                bullet = null;
            }
        }
        
        
        customShapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Asteroid a : asteroids) {
            a.renderBody(customShapeRenderer);
        }
        customShapeRenderer.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Iterator<Asteroid> asteroidIterator = new Array.ArrayIterator<>(asteroids); asteroidIterator.hasNext(); ) {
            Asteroid a = asteroidIterator.next();
            a.render(shape, delta);
            
            
            for (Asteroid b : new Array.ArrayIterator<>(asteroids)) {
                if (a.equals(b)) continue;
                
                if (a.hullPoly.getBoundingRectangle().overlaps(b.hullPoly.getBoundingRectangle())) {
                    /*
                    while (a.hullPoly.getBoundingRectangle().overlaps(b.hullPoly.getBoundingRectangle())) {
                       a.position.sub(2, 0);
                       b.position.add(2, 0);
                       a.hullPoly.setPosition(a.position.x, b.position.y);
                       b.hullPoly.setPosition(a.position.x, b.position.y);
                    }*/
                    
                    a.angle -= 180 * MathUtils.degRad;
    
                    //float angle = a.position.angleRad(b.position);
                    //a.angle = -angle;
                }
                /*
                while (a.hullPoly.getBoundingRectangle().overlaps(b.hullPoly.getBoundingRectangle())) {
                    a.position.sub(2, 0);
                    b.position.add(2, 0);
                    a.hullPoly.setPosition(a.position.x, b.position.y);
                    b.hullPoly.setPosition(a.position.x, b.position.y);
                }*/
            }
            
            if (bullet != null) {
                if (a.hullPoly.contains(bullet)) {
                    if (a.hullPoly.area() > 600) {
                        int size = (int) (a.size * 0.65f);
                        Asteroid asteroidA = new Asteroid(a.position.cpy(), size, mouseAngle + (MathUtils.random(0, 45) * MathUtils.degRad), MathUtils.random(20, 40));
                        Asteroid asteroidB = new Asteroid(a.position.cpy(), size, mouseAngle - (MathUtils.random(0, 45) * MathUtils.degRad), MathUtils.random(20, 40));
                        while (asteroidA.hullPoly.getBoundingRectangle().overlaps(asteroidB.hullPoly.getBoundingRectangle())) {
                            asteroidA.position.sub(2, 0);
                            asteroidB.position.add(2, 0);
                            asteroidA.hullPoly.setPosition(asteroidA.position.x, asteroidA.position.y);
                            asteroidB.hullPoly.setPosition(asteroidB.position.x, asteroidB.position.y);
                        }
                        asteroids.add(asteroidA);
                        asteroids.add(asteroidB);
                    }
                    
                    bullet = null;
                    asteroidIterator.remove();
                }
            }
        }
    
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            asteroids.add(new Asteroid(mousePos.cpy(), 200, 0, 0));
        }
        
        shape.end();
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        if (bullet != null) {
            bullet.add(MyMath.vector(bulletAngle, bulletVelocity * delta));
            shape.setColor(Color.BLACK);
            shape.circle(bullet.x, bullet.y, 10);
        }
        
        // draw ship
        shape.setColor(Color.WHITE);
        setShape(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2, mouseAngle);
        for(int i = 0, j = shapeX.length - 1; i < shapeY.length; j = i++) {
            shape.line(shapeX[i], shapeY[i], shapeX[j], shapeY[j]);
        
        }
        shape.end();
    }
    
    private float[] shapeX = new float[4];
    private float[] shapeY = new float[4];
    private void setShape(float x, float y, float radians) {
        float scale = 40;
        shapeX[0] = x + MathUtils.cos(radians) * scale;
        shapeY[0] = y + MathUtils.sin(radians) * scale;
        
        shapeX[1] = x + MathUtils.cos(radians - 4 * 3.1415f / 5) * scale;
        shapeY[1] = y + MathUtils.sin(radians - 4 * 3.1415f / 5) * scale;
        
        shapeX[2] = x + MathUtils.cos(radians + 3.1415f) * scale * (5.0f/8);
        shapeY[2] = y + MathUtils.sin(radians + 3.1415f) * scale * (5.0f/8);
        
        shapeX[3] = x + MathUtils.cos(radians + 4 * 3.1415f / 5) * scale;
        shapeY[3] = y + MathUtils.sin(radians + 4 * 3.1415f / 5) * scale;
    }
    
    @Override
    public void resize(int width, int height) {
    
    }
    
    private class Asteroid {
        
        Vector2 position;
        float angle, velocity;
        Polygon hullPoly;
        int size;
        
        public Asteroid(Vector2 position, int size, float angle, float velocity) {
            this.size = size;
            this.angle = angle;
            this.velocity = velocity;
            this.position = position.sub(size/2, size/2);
            
            FloatArray points = new FloatArray();
            int numPoints = 20;
            for (int i = 0; i < numPoints * 2; i += 2) {
                float x = MathUtils.random(size);
                float y = MathUtils.random(size);
                points.add(x);
                points.add(y);
            }
            
            ConvexHull convex = new ConvexHull();
            float[] hull = convex.computePolygon(points, false).toArray();
            hullPoly = new Polygon(hull);
            hullPoly.setOrigin(size / 2, size / 2);//should actually be center of mass//TODO: lookup center of mass for arbitrary poly
        }
        
        public void render(ShapeRenderer shape, float delta) {
            //float angle = MyMath.angleTo(new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY()), position);
            //position.add(MyMath.vector(angle,10*delta));
            
            Rectangle bounds = hullPoly.getBoundingRectangle();
            if (bounds.y <= 0) {
                position.add(0, 1);
                angle = MathUtils.PI2 - angle;
            } else if (bounds.y + bounds.height >= Gdx.graphics.getHeight()) {
                position.sub(0, 1);
                angle = MathUtils.PI2 - angle;
            }
        //shape.triangle();
            if (bounds.x <= 0) {
                position.add(1, 0);
                angle = MathUtils.PI - angle;
            } else if (bounds.x + bounds.width >= Gdx.graphics.getWidth()) {
                position.sub(1, 0);
                angle = MathUtils.PI - angle;
            }
            
            position.add(MyMath.vector(angle, velocity * delta));
            
            
            hullPoly.rotate(10 * delta);
            hullPoly.setPosition(position.x, position.y);
            shape.setColor(Color.BLACK);
            shape.polyline(hullPoly.getTransformedVertices());
            
            shape.setColor(Color.RED);
            Rectangle rectangle = bounds;
            shape.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }
        
        public void renderBody(CustomShapeRenderer shape) {
            shape.fillPolygon(hullPoly.getTransformedVertices(), 0, hullPoly.getVertices().length, Color.WHITE);
        }
    }
    
}
