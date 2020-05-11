package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyMath;

public class CharacterControlSystem extends IteratingSystem {
    
    private EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private ImmutableArray<Entity> vehicles;
    
    private float offsetDist = 1.5f;//TODO: dynamic based on ship size
    private float faceRotSpeed = 8f;//move to CharacterControlSystemConfig
    
    public CharacterControlSystem() {
        super(Family.all(ControllableComponent.class, TransformComponent.class, CharacterComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        controlCharacter(entity, deltaTime);
    }
    
    private void controlCharacter(Entity entity,  float delta) {
        ControllableComponent control = Mappers.controllable.get(entity);
        CharacterComponent character = Mappers.character.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        PhysicsComponent physicsComp = Mappers.physics.get(entity);
    
        control.canTransition = false;
        
        //make character face mouse/joystick
        faceMouse(control, physicsComp, delta);
        
        //todo: test moving control into relevant component? eg: if (character.walk) instead?
        if (control.moveForward) {
            float walkSpeed = character.walkSpeed * control.movementMultiplier * delta;
            physicsComp.body.applyForceToCenter(MyMath.vector(transform.rotation, walkSpeed), true);
        }
        
        //character.enterVehicle
        if (control.changeVehicle) {
            tryEnterVehicle(entity, control);
        }
    }
    
    private void faceMouse(ControllableComponent control, PhysicsComponent physicsComp, float delta) {
        //make vehicle face angle from mouse/joystick
        float angle = MathUtils.lerpAngle(physicsComp.body.getAngle(), control.angleFacing, faceRotSpeed * delta);
        float impulse = MyMath.getAngularImpulse(physicsComp.body, angle, delta);
        physicsComp.body.applyAngularImpulse(impulse, true);
    }
    
    private void tryEnterVehicle(Entity characterEntity, ControllableComponent control) {
        if (!control.timerVehicle.canDoEvent())
            return;
        
        control.changeVehicle = false;
        
        //get all vehicles and check if player is close to one
        PhysicsComponent playerPhysics = Mappers.physics.get(characterEntity);
        for (Entity vehicleEntity : vehicles) {
            
            //skip vehicle is occupied //isVehicleOcupado?
            if (Mappers.vehicle.get(vehicleEntity).driver != null) {
                Gdx.app.log(this.getClass().getSimpleName(), "Vehicle [" + Misc.objString(vehicleEntity)
                        + "] already has a driver [" + Misc.objString(Mappers.vehicle.get(vehicleEntity).driver) + "]!");
                continue;
            }
            
            //check if character is near a vehicle
            PhysicsComponent vehiclePhysics = Mappers.physics.get(vehicleEntity);
            if (playerPhysics.body.getPosition().dst(vehiclePhysics.body.getPosition()) < offsetDist) {
                
                enterVehicle(characterEntity, vehicleEntity);
                
                control.timerVehicle.reset();
                
                return;
            }
        }
    }
    
    private void enterVehicle(Entity characterEntity, Entity vehicle) {
        //set reference
        Mappers.vehicle.get(vehicle).driver = characterEntity;
        
        // transfer focus & controls to vehicle
        CameraFocusComponent cameraFocus = (CameraFocusComponent) ECSUtil.transferComponent(characterEntity, vehicle, CameraFocusComponent.class);
        if (cameraFocus != null) {
            // zoom out camera
            vehicle.getComponent(CameraFocusComponent.class).zoomTarget = engineCFG.defaultZoomVehicle;
        }
        ECSUtil.transferComponent(characterEntity, vehicle, ControllableComponent.class);
        ECSUtil.transferComponent(characterEntity, vehicle, AIComponent.class);
        ECSUtil.transferComponent(characterEntity, vehicle, ControlFocusComponent.class);
        
        
        // remove character
        getEngine().removeEntity(characterEntity);
        GameScreen.box2dWorld.destroyBody(characterEntity.getComponent(PhysicsComponent.class).body);//todo: try enable/disable instead of delete and recreate?
        characterEntity.getComponent(PhysicsComponent.class).body = null;
    }
    
}
