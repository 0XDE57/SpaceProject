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
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Transform;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.SpaceStationComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class SpaceStationSystem extends IteratingSystem {

    private final ShapeRenderer shape;
    private final Vector2 tempVec = new Vector2();
    

    public SpaceStationSystem() {
        super(Family.all(SpaceStationComponent.class, TransformComponent.class).get());
        shape = new ShapeRenderer();
    }
    
    @Override
    public void update(float deltaTime) {
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.setProjectionMatrix(GameScreen.cam.combined);

        super.update(deltaTime);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        //keep station in orbit around parent body, don't fling out into universe...
        SpaceStationComponent spaceStation = Mappers.spaceStation.get(entity);
        PhysicsComponent stationPhysics = Mappers.physics.get(entity);
        if (spaceStation.parentOrbitBody != null) {
            TransformComponent parentTransform = Mappers.transform.get(spaceStation.parentOrbitBody);
            //set velocity perpendicular to parent body, (simplified 2-body model)
            float angle = MyMath.angleTo(parentTransform.pos, stationPhysics.body.getPosition()) + (spaceStation.velocity > 0 ? -MathUtils.HALF_PI : MathUtils.HALF_PI);
            stationPhysics.body.setLinearVelocity(MyMath.vector(angle, spaceStation.velocity));
        }

        //update docked ships
        if (spaceStation.dockPortA != null) {
            updateShipInDock(spaceStation, stationPhysics, spaceStation.dockPortA, BodyBuilder.DOCK_A_ID);
        }
        if (spaceStation.dockPortB != null) {
            updateShipInDock(spaceStation, stationPhysics, spaceStation.dockPortB, BodyBuilder.DOCK_B_ID);
        }
        if (spaceStation.dockPortC != null) {
            updateShipInDock(spaceStation, stationPhysics, spaceStation.dockPortC, BodyBuilder.DOCK_C_ID);
        }
        if (spaceStation.dockPortD != null) {
            updateShipInDock(spaceStation, stationPhysics, spaceStation.dockPortD, BodyBuilder.DOCK_D_ID);
        }

        //draw docking pad
        shape.begin(ShapeRenderer.ShapeType.Filled);
        Transform transform = stationPhysics.body.getTransform();
        for (Fixture fixture : stationPhysics.body.getFixtureList()) {
            if (fixture.getUserData() == null) continue;

            shape.setColor(Color.BLACK);
            int dockID = (int) fixture.getUserData();
            if (spaceStation.dockPortA != null && dockID == BodyBuilder.DOCK_A_ID) {
                shape.setColor(Color.WHITE);
            }
            if (spaceStation.dockPortB != null && dockID == BodyBuilder.DOCK_B_ID) {
                shape.setColor(Color.WHITE);
            }
            if (spaceStation.dockPortC != null && dockID == BodyBuilder.DOCK_C_ID) {
                shape.setColor(Color.WHITE);
            }
            if (spaceStation.dockPortD != null && dockID == BodyBuilder.DOCK_D_ID) {
                shape.setColor(Color.WHITE);
            }

            CircleShape dock = (CircleShape) fixture.getShape();
            tempVec.set(dock.getPosition());
            transform.mul(tempVec);
            shape.circle(tempVec.x, tempVec.y, dock.getRadius());
        }
        shape.end();
    }

    private void updateShipInDock(SpaceStationComponent spaceStation, PhysicsComponent stationPhysics, Entity dockedShip, int dockId) {
        PhysicsComponent shipPhysics = Mappers.physics.get(dockedShip);
        Transform transform = stationPhysics.body.getTransform();
        
        //undock
        ControllableComponent control = Mappers.controllable.get(dockedShip);
        if (spaceStation.lastDockedTimer.canDoEvent() && (control.moveForward || control.moveRight || control.moveLeft || control.boost)) {
            if (spaceStation.dockPortA == dockedShip) {
                spaceStation.dockPortA = null;
                shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
                Gdx.app.debug(getClass().getSimpleName(), "undock port: A");
                control.activelyControlled = true;
            }
            if (spaceStation.dockPortB == dockedShip) {
                spaceStation.dockPortB = null;
                shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
                Gdx.app.debug(getClass().getSimpleName(), "undock port: B");
                control.activelyControlled = true;
            }
            if (spaceStation.dockPortC == dockedShip) {
                spaceStation.dockPortC = null;
                shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
                Gdx.app.debug(getClass().getSimpleName(), "undock port: C");
                control.activelyControlled = true;
            }
            if (spaceStation.dockPortD == dockedShip) {
                spaceStation.dockPortD = null;
                shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
                Gdx.app.debug(getClass().getSimpleName(), "undock port: D");
                control.activelyControlled = true;
            }
        }
        
        //update ship position relative to space station
        for (Fixture fixture : stationPhysics.body.getFixtureList()) {
            if (fixture.getUserData() == null) continue;
            
            if ((int)fixture.getUserData() == dockId) {
                CircleShape dock = (CircleShape) fixture.getShape();
                tempVec.set(dock.getPosition());
                transform.mul(tempVec);
                shipPhysics.body.setTransform(tempVec, shipPhysics.body.getAngle());
                shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
            }
        }
    }

}
