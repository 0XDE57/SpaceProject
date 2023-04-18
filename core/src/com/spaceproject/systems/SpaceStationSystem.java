package com.spaceproject.systems;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Transform;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.SpaceStationComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;

public class SpaceStationSystem extends EntitySystem {
    
    private final Vector2 tempVec = new Vector2();
    private ImmutableArray<Entity> stations;
    
    @Override
    public void addedToEngine(Engine engine) {
        stations = engine.getEntitiesFor(Family.all(SpaceStationComponent.class, TransformComponent.class).get());
    }
    
    @Override
    public void update(float deltaTime) {
        for (Entity entity : stations) {
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
            if (spaceStation.dockedPortA != null) {
                updateShipInDock(spaceStation, stationPhysics, spaceStation.dockedPortA, BodyBuilder.DOCK_A_ID);
            }
            if (spaceStation.dockedPortB != null) {
                updateShipInDock(spaceStation, stationPhysics, spaceStation.dockedPortB, BodyBuilder.DOCK_B_ID);
            }
        }
    }
    
    private void updateShipInDock(SpaceStationComponent spaceStation, PhysicsComponent stationPhysics, Entity dockedShip, int dockId) {
        PhysicsComponent shipPhysics = Mappers.physics.get(dockedShip);
        Transform transform = stationPhysics.body.getTransform();
        
        //undock
        if (Mappers.controllable.get(dockedShip).interact) {
            if (spaceStation.dockedPortA == dockedShip) {
                spaceStation.dockedPortA = null;
                shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
                Gdx.app.debug(getClass().getSimpleName(), "undock port A");
            }
            if (spaceStation.dockedPortB == dockedShip) {
                spaceStation.dockedPortB = null;
                shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
                Gdx.app.debug(getClass().getSimpleName(), "undock port B");
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
