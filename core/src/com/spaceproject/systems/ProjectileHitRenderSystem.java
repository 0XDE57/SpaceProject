package com.spaceproject.systems;


import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.spaceproject.screens.GameScreen;

import java.util.Iterator;

public class ProjectileHitRenderSystem extends EntitySystem implements Disposable {

    static class Ring implements Pool.Poolable {
        public float x, y;
        public float velX, velY;
        public float radius;
        public Color color = new Color();

        public void init(float x, float y, float velX, float velY, Color color) {
            this.x = x;
            this.y = y;
            this.velX = velX;
            this.velY = velY;
            this.radius = 0;
            this.color.set(color);
        }

        @Override
        public void reset() {
            //color = null;
            //recommends null but what about simply resting values?
            //color.set(1, 1, 1, 1);
            //or even ignore since it will be set on init?
        }
    }

    private static final Pool<Ring> ringPool = Pools.get(Ring.class, 200);
    private static final Array<Ring> activeRings = new Array<>();

    private final ShapeRenderer shape;
    private final float growthRate = 2.0f;

    public ProjectileHitRenderSystem() {
        shape = new ShapeRenderer();
    }

    public static void hit(float x, float y, float velX, float velY, Color color) {
        //https://libgdx.com/wiki/articles/memory-management#object-pooling
        Ring ring = ringPool.obtain();
        ring.init(x,y, velX, velY, color);
        activeRings.add(ring);
    }

    public static void hit(float x, float y, Color color) {
        hit(x, y, 0, 0, color);
    }

    int numVert;
    @Override
    public void update(float delta) {
        shape.setProjectionMatrix(GameScreen.cam.combined);
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        //render
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (Iterator<Ring> iterator = activeRings.iterator(); iterator.hasNext();) {
            Ring ring = iterator.next();
            ring.x += ring.velX * delta;
            ring.y += ring.velY * delta;
            ring.radius += growthRate * delta;
            shape.setColor(ring.color.r, ring.color.g, ring.color.b, 1-ring.radius);
            shape.circle(ring.x, ring.y, ring.radius);

            if (1 - ring.radius <= 0) {
                iterator.remove();
                ringPool.free(ring);
            }
        }
        numVert = shape.getRenderer().getNumVertices(); //must be checked before end() called
        shape.end();
        
        //disable transparency
        Gdx.gl.glDisable(GL20.GL_BLEND);

        DebugSystem.addDebugText(toString(), 100, 100);
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }

    StringBuilder infoString = new StringBuilder();
    @Override
    public String toString() {
        infoString.setLength(0);
        infoString.append("vertices: ").append(numVert)
                .append(", [Ring Pool] active: ").append(activeRings.size)
                .append(", free: ").append(ringPool.getFree())
                .append(", peak: ").append(ringPool.peak)
                .append(", max: ").append(ringPool.max);
        return infoString.toString();
    }
    
}
