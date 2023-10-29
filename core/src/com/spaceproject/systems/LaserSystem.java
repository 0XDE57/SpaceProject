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
import com.badlogic.gdx.utils.ScreenUtils;
import com.spaceproject.components.LaserComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class LaserSystem extends IteratingSystem implements Disposable, RayCastCallback {

    final Vector2 incidentRay = new Vector2();
    final Vector2 castPoint = new Vector2();
    final Vector2 castNormal = new Vector2();
    final ShapeRenderer shape;
    boolean reflecting = false;

    boolean drawNormal = false;

    public LaserSystem() {
        super(Family.all(LaserComponent.class, PhysicsComponent.class).get());
        shape = new ShapeRenderer();
    }

    @Override
    public void update(float deltaTime) {
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
        laser.a.set(body.getPosition());
        incidentRay.set(MyMath.vector(body.getAngle(), laser.maxLaserDist).add(laser.a));
        reflecting = false;
        castPoint.set(incidentRay);
        GameScreen.box2dWorld.rayCast(this, laser.a, incidentRay);
        laser.b.set(castPoint);
        //shape.setColor(reflecting ? Color.RED : Color.FIREBRICK);
        //shape.rectLine(laser.a, laser.b, 0.3f);
        Color startColor = Color.RED;
        Color endColor = Color.CLEAR.cpy().lerp(Color.FIREBRICK, MathUtils.random()*0.25f);

        Vector2 incidentVector = new Vector2(laser.b).sub(laser.a);
        float distanceToHit = incidentVector.len();
        Color ratio = startColor.cpy().lerp(endColor, (distanceToHit / laser.maxLaserDist) - MathUtils.random()*0.15f);
        shape.rectLine(laser.a.x, laser.a.y, laser.b.x, laser.b.y, 0.3f, startColor, ratio);
        //shape.setColor(Color.SLATE);
        //shape.rectLine(laser.a, laser.b, 0.1f);

        if (reflecting) {
            if (drawNormal) {
                //draw normal
                incidentRay.set(MyMath.vector(castNormal.angleRad(), laser.maxLaserDist).add(laser.b));
                shape.setColor(Color.GREEN);
                shape.rectLine(laser.b, incidentRay, 0.3f);
                shape.setColor(Color.SLATE);
                shape.rectLine(laser.b, incidentRay, 0.1f);
            }

            //calculate reflection
            Vector2 reflectedVector = new Vector2(incidentVector).sub(castNormal.scl(2 * incidentVector.dot(castNormal))).setLength(laser.maxLaserDist - distanceToHit).add(laser.b);
            castPoint.set(reflectedVector);
            reflecting = false;
            GameScreen.box2dWorld.rayCast(this, laser.b, reflectedVector);
            //shape.setColor(Color.RED);
            //shape.rectLine(laser.b, castPoint, 0.3f);
            shape.rectLine(laser.b.x, laser.b.y, castPoint.x, castPoint.y, 0.3f, ratio, endColor);
            //shape.setColor(Color.SLATE);
            //shape.rectLine(laser.b, castPoint, 0.1f);

            if (reflecting) {

            }
        }
    }

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        if (fixture.isSensor()) return -1;
        castPoint.set(point);
        castNormal.set(normal);
        reflecting = true;
        //Gdx.app.debug(getClass().getSimpleName(), MyMath.formatVector2(point, 1) + " - " + MyMath.formatVector2(normal, 1));
        return fraction;
    }

    @Override
    public void dispose() {
        shape.dispose();
    }

}
