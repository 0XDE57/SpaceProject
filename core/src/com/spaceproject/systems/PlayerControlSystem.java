package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Intersector;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.utility.Mappers;

public class PlayerControlSystem extends EntitySystem {

	private static Engine engine;

	//target reference
	private Entity playerEntity = null; //the player entity
	private Entity vehicleEntity = null;//the vehicle player currently controls (also inVehicle flag if !null)

	//vehicles array to check if player can get in 
	private ImmutableArray<Entity> vehicles;

	//action timer, for enter/exit vehicle
	//TODO: move to component, both player and AI need to be able to enter/exit
	private float timeSinceVehicle = 0;
	private int timeTillCanGetInVehicle = 60;
		
	//player should shoot
	public boolean shoot = false;

	//vehicle should move
	public boolean moveForward = false; 
	public boolean moveLeft = false;
	public boolean moveRight = false;
	public boolean applyBreaks = false;
	
	//for analog control. will be value between 1 and 0
	public float movementMultiplier = 0; 
	//if vehicle should stop instantly-debug stop
	public boolean stop = false;
	
	//set direction player faces
	public float angleFacing = 0;
	
	public PlayerControlSystem(Entity player) {
		this.playerEntity = player;	
	}
	
	public PlayerControlSystem(Entity player, Entity vehicle) {
		this(player);
		this.vehicleEntity = vehicle;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;	
		
		vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());	
	}


	@Override
	public void update(float delta) {		
		
		//getting in and out of vehicle timer
		//TODO: variables could use better names
		if (timeSinceVehicle < timeTillCanGetInVehicle) {
			timeSinceVehicle += 100 * delta;
		}
		
	
		//VEHICLE CONRTROLS////////////////////////////////////////////////////////////////////
		if (isInVehicle()) {
			
			//vehicle position
			TransformComponent vehicleTransform = Mappers.transform.get(vehicleEntity);
			//vehicle movement
			MovementComponent vehicleMovement = Mappers.movement.get(vehicleEntity);	
			
			VehicleComponent vehicle = Mappers.vehicle.get(vehicleEntity);
			
			CannonComponent vehicleCannon = Mappers.cannon.get(vehicleEntity);
			//TODO: cannon refill logic needs to be moved to system, all ships need to recharge
			//deal with cannon timers
			vehicleCannon.timeSinceLastShot -= 100 * delta;
			vehicleCannon.timeSinceRecharge -= 100 * delta;
			refillAmmo(vehicleCannon);
			
			//make vehicle face angle from mouse/joystick
			vehicleTransform.rotation = angleFacing;	
			
			//apply thrust forward
			if (moveForward) {
				//TODO: implement rest of engine behavior
				//float maxSpeed;
				//float maxSpeedMultiplier? on android touch controls make maxSpeed be relative to finger distance so that finger distance determines how fast to go
				float thrust = vehicle.thrust;
				float angle = vehicleTransform.rotation;
				float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
				float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
				vehicleMovement.velocity.add(dx, dy);
			}
			
			//apply thrust left
			if (moveLeft) {
				float thrust = vehicle.thrust * 0.6f;
				float angle = vehicleTransform.rotation + 1.57f;
				float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
				float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
				vehicleMovement.velocity.add(dx, dy);
			}
			
			//apply thrust right
			if (moveRight) {
				float thrust = vehicle.thrust * 0.6f;
				float angle = vehicleTransform.rotation - 1.57f;
				float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
				float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
				vehicleMovement.velocity.add(dx, dy);
			}
			
			//apply breaks
			if (applyBreaks) {					
				if (vehicleMovement.velocity.len() < 1) {
					vehicleMovement.velocity.set(0,0);
				} else {
					float thrust = vehicle.thrust * 0.9f;
					float angle = vehicleMovement.velocity.angle();
					float dx = (float) Math.cos(angle) * thrust * delta;
					float dy = (float) Math.sin(angle) * thrust * delta;
					vehicleMovement.velocity.add(dx, dy);
				}
			}
			
			//ATTACK/cannon-----------------------
			if (shoot && canFire(vehicleCannon)) {							
				fireCannon(vehicleTransform, vehicleMovement, vehicleCannon, vehicleEntity.getId());
			}
			
			if (stop) {//debug stop
				vehicleMovement.velocity.set(0,0);
				stop = false;
			}
					
		} else { 
			//CHARACTER CONTROLS///////////////////////////////////////////////////////////////////////////
			
			//players position
			TransformComponent playerTransform = Mappers.transform.get(playerEntity);
			
			//make character face mouse/joystick
			playerTransform.rotation = angleFacing;
						
			if (moveForward) {				
				float walkSpeed = 35f; //TODO: move to component
				float dx = (float) Math.cos(playerTransform.rotation) * (walkSpeed * movementMultiplier) * delta;
				float dy = (float) Math.sin(playerTransform.rotation) * (walkSpeed * movementMultiplier) * delta;
				
				playerTransform.pos.add(dx, dy, 0);
			}
			
			
		}
	}

	private void refillAmmo(CannonComponent vehicleCan) {
		if (vehicleCan.timeSinceRecharge < 0 && vehicleCan.curAmmo < vehicleCan.maxAmmo) {
			//refill ammo
			vehicleCan.curAmmo++;		
			
			//reset timer
			vehicleCan.timeSinceRecharge = vehicleCan.rechargeRate;
		}
	}

	/**
	 * Fire cannon.
	 * @param vehicleTransform
	 * @param vehicleMovement
	 * @param vehicleCan
	 */
	private void fireCannon(TransformComponent vehicleTransform, MovementComponent vehicleMovement, CannonComponent vehicleCan, long ID) {
		//reset timer if ammo is full, to prevent instant recharge
		if (vehicleCan.curAmmo == vehicleCan.maxAmmo) {			
			vehicleCan.timeSinceRecharge = vehicleCan.rechargeRate;
		}		
		
		//create missile	
		float dx = (float) (Math.cos(vehicleTransform.rotation) * vehicleCan.velocity) + vehicleMovement.velocity.x;
		float dy = (float) (Math.sin(vehicleTransform.rotation) * vehicleCan.velocity) + vehicleMovement.velocity.y;
		engine.addEntity(EntityFactory.createMissile(vehicleTransform, dx, dy, vehicleCan.size, vehicleCan.damage, ID));
		
		//subtract ammo
		--vehicleCan.curAmmo;
		
		//reset timer
		vehicleCan.timeSinceLastShot = vehicleCan.fireRate;

		
		/*
		 * Cheat for debug:
		 * fast firing and infinite ammo
		 */
		boolean cheat = false;
		if (cheat) {
			vehicleCan.curAmmo++;
			vehicleCan.timeSinceLastShot = -1;
		}
	}

	/**
	 * Check if has enough ammo and time past since last shot.
	 * @param vehicleCan
	 * @return true if can fire
	 */
	private boolean canFire(CannonComponent vehicleCan) {
		return vehicleCan.curAmmo > 0 && vehicleCan.timeSinceLastShot <= 0;
	}
	
	
	/**
	 * Enter nearest vehicle if available.
	 */
	public void enterVehicle() {
		//check if already in vehicle
		if (isInVehicle()) {
			return;
		}
		
		//action timer
		if (timeSinceVehicle < timeTillCanGetInVehicle) {
			return;
		} else {
			timeSinceVehicle = 0;
		}
		
		//get all vehicles and check if player is close to one(bounds overlap)
		for (int v = 0; v < vehicles.size(); v++) {
			Entity vehicle = vehicles.get(v);
			BoundsComponent vehicleBounds = Mappers.bounds.get(vehicles.get(v));
			BoundsComponent playerBounds = Mappers.bounds.get(playerEntity);
			
			//TODO should this be in collision detection class? use listeners?
			//check if character near vehicle
			if (playerBounds.poly.getBoundingRectangle().overlaps(vehicleBounds.poly.getBoundingRectangle())) {			
				if (Intersector.overlapConvexPolygons(vehicleBounds.poly, playerBounds.poly)){
					// get in vehicle
					
					//TODO: find better way to do this, check entity for vehicle component
					vehicleEntity = vehicle; //set vehicle reference

					//zoom out camera
					engine.getSystem(RenderingSystem.class).zoom(1);
					//engine.getSystem(RenderingSystem.class).pan(vehicleTransform);
					
					//set focus to vehicle
					vehicle.add(playerEntity.remove(PlayerFocusComponent.class));//TODO make switch focus method (oldEntity, newEntity)
				
					//remove player from engine
					engine.removeEntity(playerEntity);
				}
			}
		}
	}
			
	/**
	 * Exit current vehicle.
	 */
	public void exitVehicle() {
		//check if not in vehicle
		if (!isInVehicle()) {
			return;
		}
		
		//action timer
		if (timeSinceVehicle < timeTillCanGetInVehicle) {
			return;
		} else {
			timeSinceVehicle = 0;
		}
		
		//TODO: check if can exit on platform/spacestation?
		//(dont want to get out of ship in middle of space, or do we (jetpack / personal propulsion)?
		
		//add player to engine
		engine.addEntity(playerEntity);

		//set the player at the position of vehicle
		Mappers.transform.get(playerEntity).pos.set(Mappers.transform.get(vehicleEntity).pos);				

		//zoom in camera
		engine.getSystem(RenderingSystem.class).zoom(0.4f);
		
		//set focus to player entity
		playerEntity.add(vehicleEntity.remove(PlayerFocusComponent.class));
		
		vehicleEntity = null;
		
	}

	//check if player is in vehicle
	public boolean isInVehicle() {
		return vehicleEntity != null ? true : false;
	}

}
