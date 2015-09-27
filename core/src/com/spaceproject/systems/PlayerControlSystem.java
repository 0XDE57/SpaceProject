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
	 
	//if vehicle should stop instantly-debug stop
	public boolean stop = false;
	
	//player should enter/exit vehicle
	public boolean changeVehicle = false;
	
	//for analog control. will be value between 1 and 0
	public float movementMultiplier = 0;
	
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
		//TODO: move to component
		if (timeSinceVehicle < timeTillCanGetInVehicle) {
			timeSinceVehicle += 100 * delta;
		}
		
	
		if (isInVehicle()) {			
			controlShip(delta);					
		} else { 			
			controlCharacter(delta);			
		}
		
		if (changeVehicle) {
			if (isInVehicle()) {
				exitVehicle();
			} else {
				enterVehicle();
			}
		}
	}

	/**
	 * Control the character.
	 * @param delta
	 */
	private void controlCharacter(float delta) {
		//players position
		TransformComponent transform = Mappers.transform.get(playerEntity);
		
		//make character face mouse/joystick
		transform.rotation = angleFacing;
					
		if (moveForward) {				
			float walkSpeed = 35f; //TODO: move to component
			float dx = (float) Math.cos(transform.rotation) * (walkSpeed * movementMultiplier) * delta;
			float dy = (float) Math.sin(transform.rotation) * (walkSpeed * movementMultiplier) * delta;
			
			transform.pos.add(dx, dy, 0);
		}
	}

	/**
	 * Control the ship.
	 * @param delta
	 */
	private void controlShip(float delta) {
		TransformComponent transform = Mappers.transform.get(vehicleEntity);
		MovementComponent movement = Mappers.movement.get(vehicleEntity);	
		VehicleComponent vehicle = Mappers.vehicle.get(vehicleEntity);
		
		CannonComponent cannon = Mappers.cannon.get(vehicleEntity);
		//TODO: cannon refill logic needs to be moved to system, all ships need to recharge
		//deal with cannon timers
		cannon.timeSinceLastShot -= 100 * delta;
		cannon.timeSinceRecharge -= 100 * delta;
		refillAmmo(cannon);
		
		//make vehicle face angle from mouse/joystick
		transform.rotation = angleFacing;	
		
		//apply thrust forward
		if (moveForward) {
			//TODO: implement rest of engine behavior
			//float maxSpeed;
			//float maxSpeedMultiplier? on android touch controls make maxSpeed be relative to finger distance so that finger distance determines how fast to go
			float thrust = vehicle.thrust;
			float angle = transform.rotation;
			float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
			float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
			movement.velocity.add(dx, dy);
		}
		
		//apply thrust left
		if (moveLeft) {
			float thrust = vehicle.thrust * 0.6f;
			float angle = transform.rotation + 1.57f;
			float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
			float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
			movement.velocity.add(dx, dy);
		}
		
		//apply thrust right
		if (moveRight) {
			float thrust = vehicle.thrust * 0.6f;
			float angle = transform.rotation - 1.57f;
			float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
			float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
			movement.velocity.add(dx, dy);
		}
		
		//apply breaks
		if (applyBreaks) {					
			if (movement.velocity.len() < 10) {
				movement.velocity.set(0,0);
			} else {
				float thrust = movement.velocity.len();
				if (thrust > 1000) thrust = 1000; //cap the breaking power
				float angle = movement.velocity.angle();
				float dx = (float) Math.cos(angle) * thrust * delta;
				float dy = (float) Math.sin(angle) * thrust * delta;
				movement.velocity.add(dx, dy);
			}
		}
		
		//fire cannon / attack
		if (shoot && canFire(cannon)) {							
			fireCannon(transform, movement, cannon, vehicleEntity.getId());
		}
		
		//debug stop
		if (stop) {
			movement.velocity.set(0,0);
			stop = false;
		}
	}

	/**
	 * Refill ammo for the cannon
	 * @param cannon
	 */
	private void refillAmmo(CannonComponent cannon) {
		if (cannon.timeSinceRecharge < 0 && cannon.curAmmo < cannon.maxAmmo) {
			//refill ammo
			cannon.curAmmo++;		
			
			//reset timer
			cannon.timeSinceRecharge = cannon.rechargeRate;
		}
	}

	/**
	 * Fire cannon.
	 * @param transform of ship
	 * @param movement of ship
	 * @param cannon
	 */
	private void fireCannon(TransformComponent transform, MovementComponent movement, CannonComponent cannon, long ID) {
		//reset timer if ammo is full, to prevent instant recharge
		if (cannon.curAmmo == cannon.maxAmmo) {			
			cannon.timeSinceRecharge = cannon.rechargeRate;
		}		
		
		//create missile	
		float dx = (float) (Math.cos(transform.rotation) * cannon.velocity) + movement.velocity.x;
		float dy = (float) (Math.sin(transform.rotation) * cannon.velocity) + movement.velocity.y;
		engine.addEntity(EntityFactory.createMissile(transform, dx, dy, cannon.size, cannon.damage, ID));
		
		//subtract ammo
		--cannon.curAmmo;
		
		//reset timer
		cannon.timeSinceLastShot = cannon.fireRate;
		
		/*
		 * Cheat for debug:
		 * fast firing and infinite ammo
		 */
		boolean cheat = false;
		if (cheat) {
			cannon.curAmmo++;
			cannon.timeSinceLastShot = -1;
		}
	}

	/**
	 * Check if has enough ammo and time past since last shot.
	 * @param cannon
	 * @return true if can fire
	 */
	private boolean canFire(CannonComponent cannon) {
		return cannon.curAmmo > 0 && cannon.timeSinceLastShot <= 0;
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
					//TODO make switch focus method (oldEntity, newEntity)
					vehicle.add(playerEntity.remove(PlayerFocusComponent.class));
				
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

	/**
	 * Check if player is in vehicle.
	 * @return true if in vehicle
	 */
	public boolean isInVehicle() {
		return vehicleEntity != null;
	}

}
