package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyMath;

public class AISystem extends IteratingSystem {

	private Engine engine;
	private ImmutableArray<Entity> vehicles;
	
	public AISystem() {
		super(Family.all(AIComponent.class, ControllableComponent.class).get());
	}
	
	@Override
	public void addedToEngine(Engine engine) {		
		super.addedToEngine(engine);
		vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
		this.engine = engine;
	}

	protected void processEntity(Entity entity, float delta) {
		/* simple DUMB test "AI" for now just to create structure/skeleton
		 * and test some basic behaviors
		 */
		AIComponent ai = Mappers.AI.get(entity);
		ControllableComponent control = Mappers.controllable.get(entity);
		
		//follow player
		Entity player = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get()).first();
		Vector3 pPos = Mappers.transform.get(player).pos;
		Vector3 aiPos = Mappers.transform.get(entity).pos;
		control.angleFacing = MyMath.angleTo(pPos, aiPos);
		control.moveForward = true;
		control.movementMultiplier = 1f;
		
		/*
		//follow closet vehicle
		Entity closestVehicle = Misc.closestEntity(aiPos, vehicles);
		if (closestVehicle != null) {
			control.angleFacing = MyMath.angleTo(Mappers.transform.get(closestVehicle).pos,aiPos);
		}
		*/
		
	}
}
