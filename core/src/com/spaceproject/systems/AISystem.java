package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;

public class AISystem extends IteratingSystem {

	private Engine engine;
	private ImmutableArray<Entity> vehicles;
	//private ImmutableArray<Entity> players;
	
	public AISystem() {
		super(Family.all(AIComponent.class, ControllableComponent.class).get());
	}
	
	@Override
	public void addedToEngine(Engine engine) {		
		super.addedToEngine(engine);
		vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
		//players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
		this.engine = engine;
	}

	protected void processEntity(Entity entity, float delta) {
		/* simple DUMB test "AI" for now just to create structure/skeleton
		 * and test some basic behaviors
		 */
		AIComponent ai = Mappers.AI.get(entity);
		ControllableComponent control = Mappers.controllable.get(entity);
		Vector3 aiPos = Mappers.transform.get(entity).pos;
		
		//aiPos.y += 100 * delta;
		
		
		if (ai.attackTarget != null) {
			
			VehicleComponent vehicle = Mappers.vehicle.get(entity);
			if (vehicle == null) {
				Entity closestVehicle = Misc.closestEntity(aiPos, vehicles);
				if (closestVehicle != null) {
					control.angleFacing = MyMath.angleTo(Mappers.transform.get(closestVehicle).pos,aiPos);
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

				Vector3 pPos = Mappers.transform.get(ai.attackTarget).pos;
				control.angleFacing = MyMath.angleTo(pPos, aiPos);
				control.moveForward = true;
				control.movementMultiplier = 0.3f;
				control.shoot = true;
			}
		} else {
			control.shoot = false;
			
			//dumb wander
			control.angleFacing += 1 * delta;
			control.moveForward = true;
			control.movementMultiplier = 0.1f;
		}
		
		
		/*
		VehicleComponent vehicle = Mappers.vehicle.get(entity);
		if (vehicle != null) {
			// follow player
			Entity player = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get()).first();
			Vector3 pPos = Mappers.transform.get(player).pos;
			control.angleFacing = MyMath.angleTo(pPos, aiPos);
			control.moveForward = true;
			control.movementMultiplier = 0.5f;
		}

		CharacterComponent character = Mappers.character.get(entity);
		if (character != null) {
			// follow closet vehicle
			Entity closestVehicle = Misc.closestEntity(aiPos, vehicles);
			if (closestVehicle != null) {
				control.angleFacing = MyMath.angleTo(Mappers.transform.get(closestVehicle).pos,aiPos);
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
		}*/
						
	}
}