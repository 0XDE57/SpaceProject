package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.EntityFactory;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;

public class PlayerControlSystem extends EntitySystem {

	private static Engine engine;

	// movement and position maps
	private ComponentMapper<TransformComponent> transformMap;
	private ComponentMapper<MovementComponent> movementMap;
	// bounds map to check if player is near vehicle/ship
	private ComponentMapper<BoundsComponent> boundMap;
	//drivable entities
	private ComponentMapper<VehicleComponent> vehicleMap;
	//weapons
	private ComponentMapper<CannonComponent> cannonMap;
	

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
	public boolean move = false; 
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
		
		transformMap = ComponentMapper.getFor(TransformComponent.class);
		movementMap = ComponentMapper.getFor(MovementComponent.class);
		boundMap = ComponentMapper.getFor(BoundsComponent.class);
		cannonMap = ComponentMapper.getFor(CannonComponent.class);
		vehicleMap = ComponentMapper.getFor(VehicleComponent.class);
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
			TransformComponent vehicleTransform = transformMap.get(vehicleEntity);
			//vehicle movement
			MovementComponent vehicleMovement = movementMap.get(vehicleEntity);	
			
			VehicleComponent vehicle = vehicleMap.get(vehicleEntity);
			
			CannonComponent vehicleCannon = cannonMap.get(vehicleEntity);
			//deal with cannon timers
			vehicleCannon.timeSinceLastShot -= 100 * delta;
			vehicleCannon.timeSinceRechage -= 100 * delta;
			refillAmmo(vehicleCannon);
			
			//make vehicle face angle from mouse/joystick
			vehicleTransform.rotation = angleFacing - 1.57f;	
			
			//move vehicle
			if (move) {
				//add velocity in direction vehicle is facing
				//use movementMultiplier to determine how much thrust to use (analog movement)
				//TODO move to engine component
				float thrust = vehicle.thrust;
				//float maxSpeed;
				//float maxSpeedMultiplier? on android touch controls make maxSpeed be relative to finger distance so that finger distance determines how fast to go
				float dx = (float) Math.cos(vehicleTransform.rotation) * (thrust * movementMultiplier) * delta;
				float dy = (float) Math.sin(vehicleTransform.rotation) * (thrust * movementMultiplier) * delta;
				vehicleMovement.velocity.add(dx, dy);
			}
			
			//ATTACK/cannon-----------------------
			if (shoot && canFire(vehicleCannon)) {							
				fireCannon(vehicleTransform, vehicleMovement, vehicleCannon, vehicleEntity.getId());
			}
			
			if (stop) {
				vehicleMovement.velocity.set(0,0);
				stop = false;
			}
					
		} else { 
			//CHARACTER CONTROLS///////////////////////////////////////////////////////////////////////////
			
			//players position
			TransformComponent playerTransform = transformMap.get(playerEntity);
			
			//make character face mouse/joystick
			playerTransform.rotation = angleFacing - 1.57f;
						
			if (move) {				
				float walkSpeed = 35f; //TODO: move to component
				float dx = (float) Math.cos(playerTransform.rotation) * (walkSpeed * movementMultiplier) * delta;
				float dy = (float) Math.sin(playerTransform.rotation) * (walkSpeed * movementMultiplier) * delta;
				
				playerTransform.pos.add(dx, dy, 0);
			}
			
			
		}
	}

	private void refillAmmo(CannonComponent vehicleCan) {
		if (vehicleCan.timeSinceRechage < 0 && vehicleCan.curAmmo < vehicleCan.maxAmmo) {
			//refill ammo
			vehicleCan.curAmmo++;		
			
			//reset timer
			vehicleCan.timeSinceRechage = vehicleCan.rechargeRate;
		}
	}

	/**
	 * Fire cannon.
	 * @param vehicleTransform
	 * @param vehicleMovement
	 * @param vehicleCan
	 */
	private void fireCannon(TransformComponent vehicleTransform, MovementComponent vehicleMovement, CannonComponent vehicleCan, long ID) {
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
		return vehicleCan.curAmmo > 1 && vehicleCan.timeSinceLastShot <= 0;
	}


	/**
	 * Make player face point.
	 * @param x1 point 1
	 * @param y1 point 1
	 * @param x2 point 2
	 * @param y2 point 2
	 */
	public void pointTo(int x1, int y1, int x2, int y2) {
		angleFacing = (float) -(Math.atan2(x2 - x1, y2 - y1));
	}
	
	// get in vehicle
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
			BoundsComponent vehicleBounds = boundMap.get(vehicles.get(v));
			BoundsComponent playerBounds = boundMap.get(playerEntity);
			
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
			
	// get out of vehicle
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
		transformMap.get(playerEntity).pos.set(transformMap.get(vehicleEntity).pos);				

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
