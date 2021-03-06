package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.math.MyMath;

public class OrbitAnim extends TitleAnimation {
    
    Array<OrbitObject> objects;
    
    public OrbitAnim() {
        objects = new Array<OrbitObject>();
        OrbitObject center = new OrbitObject(null, 0);
        
        objects.add(center);
        for (int i = 0; i < 5; i++)
            objects.add(new OrbitObject(center, 100 + (i * 75)));
        
    }
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        shape.begin(ShapeRenderer.ShapeType.Line);
        //float i = Math.abs(objects.first().angle % MathUtils.PI2)/MathUtils.PI2;
        //System.out.println(i);
        //shape.setColor(1,1,1, i);
        shape.setColor(Color.BLACK);
        for (OrbitObject orbit : objects) {
            //orbit.update(delta);
            if (orbit.parent != null)
                shape.circle(orbit.parent.pos.x, orbit.parent.pos.y, orbit.distance, (int) orbit.distance / 4);
        }
        shape.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (OrbitObject orbit : objects) {
            orbit.update(delta, shape);
        }
        shape.end();
    }
    
    @Override
    public void resize(int width, int height) {
    }
    
    private class OrbitObject {
        private OrbitObject parent;
        private Vector2 pos;
        private float angle, rotSpeed;
        private float distance;
        private int size;
        
        public OrbitObject(OrbitObject parent, float distance) {
            this.parent = parent;
            this.distance = distance;
            angle = MathUtils.random(MathUtils.PI2);
            rotSpeed = MathUtils.random(0.15f, 0.8f);
            if (MathUtils.randomBoolean())
                rotSpeed = -rotSpeed;
            
            size = MathUtils.random(5, 10);
            if (parent == null) {
                size *= 3;
            }
            
            if (parent != null)
                pos = MyMath.vector(angle, distance).add(parent.pos);
            else
                pos = new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        }
        
        public void update(float delta, ShapeRenderer shape) {
            angle += rotSpeed * delta;
            
            if (parent != null) {
                pos = MyMath.vector(angle, distance).add(parent.pos);
                
                shape.setColor(Color.BLACK);
                shape.circle(pos.x, pos.y, size);
            } else {
                pos.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
                
                shape.setColor(Color.BLACK);
                shape.circle(pos.x, pos.y, size);
                shape.setColor(Color.WHITE);
                shape.circle(pos.x, pos.y, size - 1);
            }
            
        }
    }
    
    
}
