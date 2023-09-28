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
import com.spaceproject.components.*;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.Mappers;

public class SpaceStationSystem extends IteratingSystem {

    private final ShapeRenderer shape;
    private final Vector2 tempVec = new Vector2();
    private final float healthCostPerUnit = 15.0f; //how many credits per unit of health

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
            float angle = MyMath.angleTo(parentTransform.pos, stationPhysics.body.getPosition());
            stationPhysics.body.setLinearVelocity(MyMath.vector(angle + (spaceStation.velocity > 0 ? -MathUtils.HALF_PI : MathUtils.HALF_PI), spaceStation.velocity));
            Vector2 pos = stationPhysics.body.getPosition();
            stationPhysics.body.setTransform(pos.x, pos.y, angle);
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
            undock(spaceStation, stationPhysics, dockedShip, shipPhysics, control);
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

                CargoComponent cargo = Mappers.cargo.get(dockedShip);
                if (cargo.inventory.size() == 0) {
                    heal(cargo, Mappers.health.get(dockedShip));
                } else {
                    sellCargo(cargo);
                }
            }
        }
    }

    public void dock(Fixture shipFixture, Fixture dockFixture, Entity vehicleEntity, Entity stationEntity) {
        if (shipFixture.getUserData() != null && (int)shipFixture.getUserData() != BodyBuilder.SHIP_FIXTURE_ID) {
            return;
        }
        if (dockFixture.getUserData() == null) {
            return;
        }

        ShieldComponent shield = Mappers.shield.get(vehicleEntity);
        if (shield != null && shield.state == ShieldComponent.State.on) {
            return;
        }

        SpaceStationComponent station = Mappers.spaceStation.get(stationEntity);
        String dockedPort = "";
        switch ((int) dockFixture.getUserData()) {
            case BodyBuilder.DOCK_A_ID:
                if (station.dockPortA != null) return;//port is in use
                station.dockPortA = vehicleEntity;
                dockedPort = "A";
                break;
            case BodyBuilder.DOCK_B_ID:
                if (station.dockPortB != null) return;
                station.dockPortB = vehicleEntity;
                dockedPort = "B";
                break;
            case BodyBuilder.DOCK_C_ID:
                if (station.dockPortC != null) return;
                station.dockPortC = vehicleEntity;
                dockedPort = "C";
                break;
            case BodyBuilder.DOCK_D_ID:
                if (station.dockPortD != null) return;
                station.dockPortD = vehicleEntity;
                dockedPort = "D";
                break;
        }
        Gdx.app.debug(getClass().getSimpleName(), "dock [" + DebugUtil.objString(vehicleEntity) + "] at station: [" + DebugUtil.objString(stationEntity) + "] port: " + dockedPort);

        station.lastDockedTimer.reset();

        Mappers.physics.get(vehicleEntity).body.setLinearVelocity(0, 0);
        Mappers.controllable.get(vehicleEntity).activelyControlled = false;

        CargoComponent cargo = Mappers.cargo.get(vehicleEntity);
        cargo.lastCollectTime = GameScreen.getGameTimeCurrent();
        //sellCargo(cargo);
        //heal(cargo, Mappers.health.get(vehicleEntity));

        getEngine().getSystem(SoundSystem.class).dockStation();
    }

    private void undock(SpaceStationComponent spaceStation, PhysicsComponent stationPhysics, Entity dockedShip, PhysicsComponent shipPhysics, ControllableComponent control) {
        String port = "";
        if (spaceStation.dockPortA == dockedShip) {
            spaceStation.dockPortA = null;
            port = "A";
        }
        if (spaceStation.dockPortB == dockedShip) {
            spaceStation.dockPortB = null;
            port = "B";
        }
        if (spaceStation.dockPortC == dockedShip) {
            spaceStation.dockPortC = null;
            port = "C";
        }
        if (spaceStation.dockPortD == dockedShip) {
            spaceStation.dockPortD = null;
            port = "D";
        }

        control.activelyControlled = true;
        shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
        getEngine().getSystem(SoundSystem.class).undockStation();
        Gdx.app.debug(getClass().getSimpleName(), "undock [" + DebugUtil.objString(dockedShip) + "] from station: [" + DebugUtil.objString(spaceStation) + "] port: " + port);
    }

    private void sellCargo(CargoComponent cargo) {
        if (cargo == null) return;

        int inventoryCount = 0;
        int totalCredits = 0;
        for (ItemComponent.Resource resource : ItemComponent.Resource.values()) {
            int id = resource.getId();
            if (cargo.inventory.containsKey(id)) {
                if (GameScreen.getGameTimeCurrent() - cargo.lastCollectTime < 400) return;
                //if (!sellTimer.tryEvent()) return;
                /*todo: base local value on rarity of resource in that local system.
                   eg: a system red is common so value local value will be less,
                   or a system where gold is more rare than usual  so value will be more
                float scarcity = 1.0f;
                float localValue = scarcity * resource.getValue();
                int credits = quantity * localValue;
                */
                int quantity = cargo.inventory.get(id);
                int credits = quantity * resource.getValue();
                getEngine().getSystem(SoundSystem.class).addCredits(0.5f + ((((float) id /cargo.inventory.size()) + 1) * 0.12f));
                cargo.credits += credits;
                cargo.inventory.remove(id);
                cargo.lastCollectTime = GameScreen.getGameTimeCurrent();
                inventoryCount += quantity;
                totalCredits += credits;
                //getEngine().getSystem(SoundSystem.class).addCredits(0.6f + (((id/cargo.inventory.size()) +1) * 0.1f));
                //getEngine().getSystem(HUDSystem.class).addCredits(credits);
                Gdx.app.debug(getClass().getSimpleName(), "+" + credits + "c. sold " + quantity + " " + resource.name() + " @"+ resource.getValue() + "c");
            }
        }

        //cargo.lastCollectTime = GameScreen.getGameTimeCurrent();
        //if (totalCredits > 0) {
            //Gdx.app.debug(getClass().getSimpleName(), "total +" + totalCredits + "c for: " + inventoryCount + " units");
            //getEngine().getSystem(HUDSystem.class).addCredits(totalCredits);
            //getEngine().getSystem(SoundSystem.class).addCredits(0.5f);
        //}
    }

    private void heal(CargoComponent cargo, HealthComponent health) {
        if (cargo == null || health == null) return;
        if (health.maxHealth == health.health) return;
        if (cargo.credits <= 0) {
            Gdx.app.debug(getClass().getSimpleName(), "insufficient credits. no repairs done.");
            return;
        }

        float healthMissing = health.maxHealth - health.health;
        float healedUnits = healthMissing;
        int creditCost = (int) (healthMissing * healthCostPerUnit);
        if (creditCost > cargo.credits) {
            creditCost = cargo.credits;
            healedUnits = cargo.credits / healthCostPerUnit;
        }
        health.health += healedUnits;
        health.health = Math.min(health.health, health.maxHealth);
        cargo.credits -= creditCost;
        Gdx.app.debug(getClass().getSimpleName(), "-" + creditCost + "c for repairs: +" + (int)healedUnits + "hp restored");
    }

}
