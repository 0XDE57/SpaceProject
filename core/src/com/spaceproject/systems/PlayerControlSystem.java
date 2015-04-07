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
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.EntityFactory;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;

public class PlayerControlSystem extends EntitySystem {

	private static Engine engine;

	/* movement and position maps */
	private ComponentMapper<TransformComponent> transformMap;
	private ComponentMapper<MovementComponent> movementMap;
	/* bounds map to check if player is near vehicle/ship */
	private ComponentMapper<BoundsComponent> boundMap;


	//target reference
	private Entity playerEntity = null; //the player entity
	private Entity vehicleEntity = null;//the vehicle player currently controls (also inVehicle flag if !null)

	//vehicles array to check if player can get in 
	private ImmutableArray<Entity> vehicles;

	//action timer, for enter/exit vehicle
	private float timeSinceVehicle = 0;
	private int timeTillCanGetInVehicle = 60;
		
	//player should shoot
	public boolean shoot = false;

	//vehicle should move
	public boolean move = false; 
	//for analog control. will be value between 1 and 0
	public float movementMultiplier = 0; 
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
					
			//make vehicle face angle from mouse/joystick
			vehicleTransform.rotation = angleFacing - 1.57f;	
			
			//move vehicle----------------------------
			if (move) {
				//add velocity in direction vehicle is facing
				//use movementMultiplier to determine how much thrust to use (analog movement)
				float thrust = 320;
				float dx = (float) Math.cos(vehicleTransform.rotation) * (thrust * movementMultiplier) * delta;
				float dy = (float) Math.sin(vehicleTransform.rotation) * (thrust * movementMultiplier) * delta;
				vehicleMovement.velocity.add(dx, dy);
			}
			
			//ATTACK/Projectile-----------------------
			if (shoot) {
				//TODO fix math. The ships movement needs to be added to the projectile. It looks wrong currently
				//TODO move to weapon/projectile/gun component
				float projectileVelocity = 700;			
				float xx = (float) Math.cos(vehicleTransform.rotation) * (projectileVelocity + vehicleMovement.velocity.x);
				float yy = (float) Math.sin(vehicleTransform.rotation) * (projectileVelocity + vehicleMovement.velocity.y);
											
				engine.addEntity(EntityFactory.createProjectile(vehicleTransform.pos, xx, yy, 1));
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
				float walkSpeed = 35f; //move to component?
				float dx = (float) Math.cos(playerTransform.rotation) * (walkSpeed * movementMultiplier) * delta;
				float dy = (float) Math.sin(playerTransform.rotation) * (walkSpeed * movementMultiplier) * delta;
				
				playerTransform.pos.add(dx, dy, 0);
			}
			
			
		}
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
			System.out.println("Cannot enter vehicle: already in vehicle.");
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
			// get in vehicle
			if (playerBounds.bounds.overlaps(vehicleBounds.bounds)) {
				vehicleEntity = vehicle; //set vehicle reference

				//zoom out camera and set target to vehicle
				engine.getSystem(RenderingSystem.class).zoom(1);
				engine.getSystem(CameraSystem.class).setTarget(vehicle);
				
				//remove player from engine
				engine.removeEntity(playerEntity);
			}
		}
	}
			
	// get out of vehicle
	public void exitVehicle() {
		//check if not in vehicle
		if (!isInVehicle()) {
			System.out.println("Cannot exit vehicle: null");
			return;
		}
		
		//action timer
		if (timeSinceVehicle < timeTillCanGetInVehicle) {
			return;
		} else {
			timeSinceVehicle = 0;
		}
		
		//TODO: check if can exit on platform/spacestation 
		//(dont want to get out of ship in middle of space, or do we (jetpack / personal propulsion)?
		
		//add player to engine
		engine.addEntity(playerEntity);
		
		//set the player at the position of vehicle
		transformMap.get(playerEntity).pos.set(transformMap.get(vehicleEntity).pos);			
		 
		//remove vehicle reference
		vehicleEntity = null;

		//zoom in camera and set camera focus to player entity
		engine.getSystem(RenderingSystem.class).zoom(0.4f);
		engine.getSystem(CameraSystem.class).setTarget(playerEntity);
		//TODO refactor zoom code into camera and make it check for player in vehicle on initialization
		//and add animation/interpolation between zoom changes
	}

	//check if player is in vehicle
	public boolean isInVehicle() {
		return vehicleEntity != null ? true : false;
	}

}
