package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.*;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class LaserSystem extends IteratingSystem implements RayCastCallback, Disposable {

    final int maxReflections = 10;
    final Vector2 incidentRay = new Vector2();
    final Vector2 castPoint = new Vector2();
    final Vector2 castNormal = new Vector2();
    final Vector2 incidentVector = new Vector2();
    Fixture castFixture = null;
    final ShapeRenderer shape;
    boolean hit = false;

    int rayCount;
    int callbackCount;
    StringBuilder infoString = new StringBuilder();

    boolean drawNormal = false;
    boolean reflectAsteroidColor = false;

    public LaserSystem() {
        super(Family.all(LaserComponent.class, PhysicsComponent.class).get());
        shape = new ShapeRenderer();
    }

    @Override
    public void update(float deltaTime) {
        //reset stats
        rayCount = 0;
        callbackCount = 0;

        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.setProjectionMatrix(GameScreen.cam.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        super.update(deltaTime);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        LaserComponent laser = Mappers.laser.get(entity);
        if (laser.state == LaserComponent.State.off) return;

        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state != ShieldComponent.State.off) {
            return;
        }

        Body body = Mappers.physics.get(entity).body;
        incidentRay.set(MyMath.vector(body.getAngle(), laser.maxDist).add(body.getPosition()));
        reflect(entity, laser, body.getPosition(), incidentRay, laser.color.cpy(), laser.maxDist, maxReflections, deltaTime);
    }

    private boolean reflect(Entity entity, LaserComponent laser, Vector2 a, Vector2 b, Color color, float length, int reflections, float deltaTime) {
        if (reflections <= 0) return false;
        if (incidentVector.set(b).sub(a).len2() < 0.1f) return false;

        hit = false;
        castPoint.set(b);
        castFixture = null;
        GameScreen.box2dWorld.rayCast(this, a, b);
        rayCount++;
        b.set(castPoint);
        Color endColor = Color.CLEAR.cpy().lerp(color, MathUtils.random()*0.25f);

        incidentVector.set(b).sub(a);
        float distanceToHit = incidentVector.len();
        float range = distanceToHit / length;
        Color ratio = color.cpy().lerp(endColor, range - MathUtils.random()*0.15f);
        shape.rectLine(a.x, a.y, b.x, b.y, 0.3f, color, ratio);

        if (hit) {
            Object userData = castFixture.getBody().getUserData();
            if (userData != null) {
                Entity hitEntity = (Entity) userData;
                if (entity != hitEntity) {
                    float damage = laser.damage * (1 - range) * deltaTime;
                    Box2DContactListener.damage(getEngine(), hitEntity, entity, damage);
                }
                if (reflectAsteroidColor) {
                    AsteroidComponent asteroid = Mappers.asteroid.get(hitEntity);
                    if (asteroid != null) {
                        color.set(asteroid.color.cpy());
                    }
                }
                if (Mappers.damage.get(hitEntity) != null) {
                    hitEntity.add(new RemoveComponent());
                }
            }

            if (drawNormal) {
                //draw normal
                shape.setColor(Color.SKY);
                shape.rectLine(b, MyMath.vector(castNormal.angleRad(), length).add(b), 0.2f);
            }

            //calculate reflection: reflectedVector = incidentVector - 2 * (incidentVector dot normal) * normal
            float remainingDistance = length - distanceToHit;
            Vector2 reflectedVector = new Vector2(incidentVector).sub(castNormal.scl(2 * incidentVector.dot(castNormal))).setLength(remainingDistance).add(b);
            return reflect(entity, laser, b, reflectedVector, color, remainingDistance, --reflections, deltaTime);
        }
        return false;
    }

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        callbackCount++;
        if (fixture.isSensor()) return -1;
        castPoint.set(point);
        castNormal.set(normal);
        castFixture = fixture;
        hit = true;
        return fraction;
    }

    @Override
    public void dispose() {
        shape.dispose();
    }

    @Override
    public String toString() {
        infoString.setLength(0);
        infoString.append("[Raycasts]:         ").append(rayCount);
        infoString.append("\n[Ray callbacks]:    ").append(callbackCount);
        return infoString.toString();
    }

}
