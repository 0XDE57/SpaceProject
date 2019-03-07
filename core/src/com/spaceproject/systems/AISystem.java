package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyMath;

public class AISystem extends IteratingSystem {
    
    private ImmutableArray<Entity> vehicles;
    private ImmutableArray<Entity> planets;
    //private ImmutableArray<Entity> players;
    
    public AISystem() {
        super(Family.all(AIComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
        //players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
        planets = engine.getEntitiesFor(Family.all(PlanetComponent.class).get());
    }
    
    
    protected void processEntity(Entity entity, float delta) {
        /* simple DUMB test "AI" for now just to create structure/skeleton
         * and test some basic behaviors
         */
        AIComponent ai = Mappers.AI.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
        Vector2 aiPos = Mappers.transform.get(entity).pos;
        
        //aiPos.y += 100 * delta;
        if (ai.state == null) {
            ai.state = AIComponent.testState.dumbwander;
            //ai.state = AIComponent.testState.landOnPlanet;
        }
        
        switch (ai.state) {
            case attack:
                if (ai.attackTarget != null) {
                    
                    VehicleComponent vehicle = Mappers.vehicle.get(entity);
                    if (vehicle == null) {
                        Entity closestVehicle = Misc.closestEntity(aiPos, vehicles);
                        if (closestVehicle != null) {
                            control.angleFacing = MyMath.angleTo(Mappers.transform.get(closestVehicle).pos, aiPos);
                            control.moveForward = true;
                            control.movementMultiplier = 1f;
                            BoundsComponent aiBounds = Mappers.bounds.get(entity);
                            for (Entity v : vehicles) {
                                //skip vehicle is occupied
                                if (Mappers.vehicle.get(v).driver != null) continue;
                                
                                //check if character is near a vehicle
                                BoundsComponent vehicleBounds = Mappers.bounds.get(v);
                                if (aiBounds.poly.getBoundingRectangle().overlaps(vehicleBounds.poly.getBoundingRectangle())) {
                                    control.changeVehicle = true;
                                }
                            }
                        }
                    } else {
                        Vector2 pPos = Mappers.transform.get(ai.attackTarget).pos;
                        control.angleFacing = MyMath.angleTo(pPos, aiPos);
                        control.moveForward = true;
                        control.movementMultiplier = 0.3f;
                        control.attack = true;
                    }
                    
                } else {
                    ai.state = AIComponent.testState.dumbwander;
                }
                break;
            case dumbwander:
                control.attack = false;
                
                //dumb wander
                control.angleFacing += 1 * delta;
                control.moveForward = true;
                control.movementMultiplier = 0.1f;
                
                break;
            case landOnPlanet: {
                if (Mappers.screenTrans.get(entity) != null) {
                    ai.state = AIComponent.testState.idle;
                    control.transition = false;
                    return;
                }
                
                VehicleComponent vehicle = Mappers.vehicle.get(entity);
                if (vehicle == null) {
                    Entity closestVehicle = Misc.closestEntity(aiPos, vehicles);
                    if (closestVehicle != null) {
                        control.angleFacing = MyMath.angleTo(Mappers.transform.get(closestVehicle).pos, aiPos);
                        control.moveForward = true;
                        control.movementMultiplier = 0.5f;
                        BoundsComponent aiBounds = Mappers.bounds.get(entity);
                        for (Entity v : vehicles) {
                            //skip vehicle is occupied
                            if (Mappers.vehicle.get(v).driver != null) continue;
                            
                            //check if character is near a vehicle
                            BoundsComponent vehicleBounds = Mappers.bounds.get(v);
                            if (aiBounds.poly.getBoundingRectangle().overlaps(vehicleBounds.poly.getBoundingRectangle())) {
                                control.changeVehicle = true;
                            }
                        }
                    }
                } else {
                    ai.planetTarget = Misc.closestEntity(aiPos, planets);
                    if (ai.planetTarget != null) {
                        
                        Vector2 pPos = Mappers.transform.get(ai.planetTarget).pos;
                        control.angleFacing = MyMath.angleTo(pPos, aiPos);
                        
                        
                        if (aiPos.dst(pPos) < 200f) {
                            control.moveForward = false;
                            control.moveBack = true;
                            control.transition = true;
                            
                        } else {
                            control.moveForward = true;
                            control.movementMultiplier = 1f;
                        }
                    }
                }
                break;
            }
            case idle:
                //do nothing
				/*
				control.moveForward = false;
				control.moveBack = false;
				control.transition = false;
				control.changeVehicle = false;
				control.shoot = false;
				*/
                break;
            case follow: {
                VehicleComponent vehicle = Mappers.vehicle.get(entity);
                if (vehicle != null) {
                    // follow player
                    Entity player = getEngine().getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get()).first();
                    Vector2 pPos = Mappers.transform.get(player).pos;
                    control.angleFacing = MyMath.angleTo(pPos, aiPos);
                    control.moveForward = true;
                    control.movementMultiplier = 0.5f;
                }
                
                CharacterComponent character = Mappers.character.get(entity);
                if (character != null) {
                    // follow closet vehicle
                    Entity closestVehicle = Misc.closestEntity(aiPos, vehicles);
                    if (closestVehicle != null) {
                        control.angleFacing = MyMath.angleTo(Mappers.transform.get(closestVehicle).pos, aiPos);
                    }
                    
                    BoundsComponent playerBounds = Mappers.bounds.get(entity);
                    for (Entity v : vehicles) {
                        //skip vehicle is occupied
                        if (Mappers.vehicle.get(v).driver != null) continue;
                        
                        //check if character is near a vehicle
                        BoundsComponent vehicleBounds = Mappers.bounds.get(v);
                        if (playerBounds.poly.getBoundingRectangle().overlaps(vehicleBounds.poly.getBoundingRectangle())) {
                            control.changeVehicle = true;
                        }
                    }
                    control.moveForward = true;
                    control.movementMultiplier = 1f;
                }
                break;
            }
            case takeOffPlanet: {
                VehicleComponent vehicle = Mappers.vehicle.get(entity);
                if (vehicle == null) {
                    Entity closestVehicle = Misc.closestEntity(aiPos, vehicles);
                    if (closestVehicle != null) {
                        control.angleFacing = MyMath.angleTo(Mappers.transform.get(closestVehicle).pos, aiPos);
                        control.moveForward = false;
                        control.movementMultiplier = 0.01f;
                        BoundsComponent aiBounds = Mappers.bounds.get(entity);
                        for (Entity v : vehicles) {
                            //skip vehicle is occupied
                            if (Mappers.vehicle.get(v).driver != null) continue;
                            
                            //check if character is near a vehicle
                            BoundsComponent vehicleBounds = Mappers.bounds.get(v);
                            if (aiBounds.poly.getBoundingRectangle().overlaps(vehicleBounds.poly.getBoundingRectangle())) {
                                control.changeVehicle = true;
                            }
                        }
                    }
                } else {
                    control.transition = true;
                    ai.state = AIComponent.testState.idle;
                }
                break;
            }
        }
        
    }
}
