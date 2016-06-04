package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.LandConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.SpaceScreen;
import com.spaceproject.screens.WorldScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class PlayerControlSystem extends EntitySystem {

	private Engine engine;
	private ScreenAdapter screen;
	
	LandConfig landCFG;
	
	//target reference
	private Entity playerEntity = null; //the player entity
	private Entity vehicleEntity = null;//the vehicle player currently controls (also inVehicle flag if !null)

	//vehicles array to check if player can get in 
	private ImmutableArray<Entity> vehicles;
	private ImmutableArray<Entity> planets;

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
	
	public boolean land = false;
	private boolean animateLanding = false;
	
	//for analog control. will be value between 1 and 0
	public float movementMultiplier = 0;
	
	//set direction player faces
	public float angleFacing = 0;
	
	
	public PlayerControlSystem(ScreenAdapter screen, Entity player, LandConfig landConfig) {
		this.screen = screen;
		this.playerEntity = player;
		
		this.landCFG = landConfig;
	}
	
	public PlayerControlSystem(ScreenAdapter screen, Entity player, Entity vehicle, LandConfig landConfig) {
		this(screen, player, landConfig);
		this.vehicleEntity = vehicle;
	}


	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;	
		
		//playerEntity = engine.getEntitiesFor(Family.one(PlayerFocusComponent.class).get()).first();
		vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
		planets = engine.getEntitiesFor(Family.all(PlanetComponent.class).get());
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
		
		
		if (land && !animateLanding) {
			if (screen instanceof SpaceScreen) {
				tryLandOnPlanet();
			} else if (screen instanceof WorldScreen) {
				//TODO: Create some kind of time system so planets go to their orbital position based on time passed.
				//take off from planet
				((WorldScreen) screen).changeScreen(landCFG);
			}
			
		}
		if (animateLanding) {
			animatePlanetLanding(delta);
		}
	
	}

	private void animatePlanetLanding(float delta) {
		Entity player = engine.getEntitiesFor(Family.one(PlayerFocusComponent.class).get()).first();
		
		//freeze position
		player.getComponent(MovementComponent.class).velocity.set(0, 0); 
		
		TextureComponent tex = player.getComponent(TextureComponent.class);			
		if (tex.scale <= 0.1f) {
			tex.scale = 0;
			
			//zoom in
			engine.getSystem(SpaceRenderingSystem.class).setZoomTarget(0);
			if (engine.getSystem(SpaceRenderingSystem.class).getCamZoom() <= 0.1f) {
				//land on planet
				((SpaceScreen) screen).changeScreen(landCFG);
			}
			
		} else {
			//shrink texture
			tex.scale -= 3f * delta; 
		}
	}

	private void tryLandOnPlanet() {
		Vector3 playerPos = Mappers.transform.get(vehicleEntity).pos;
		for (Entity planet : planets) {
			Vector3 planetPos = Mappers.transform.get(planet).pos;
			TextureComponent planetTex = Mappers.texture.get(planet);
			// if player is over planet
			if (MyMath.distance(playerPos.x, playerPos.y, planetPos.x, planetPos.y) <= planetTex.texture.getWidth() / 2 * planetTex.scale) {
				animateLanding = true;

				landCFG = new LandConfig();
				landCFG.position = planetPos;// save position for taking off from planet
				landCFG.planetSeed = Mappers.planet.get(planet).seed; // save seed for planet
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
		//transform.rotation = angleFacing;
		transform.rotation = MathUtils.lerpAngle(transform.rotation, angleFacing, 6f*delta);
					
		if (moveForward) {				
			float walkSpeed = 50f; //TODO: move to component
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
		refillAmmo(cannon, delta);
		
		//make vehicle face angle from mouse/joystick
		transform.rotation = MathUtils.lerpAngle(transform.rotation, angleFacing, 8f*delta);
		
		
		//apply thrust forward accelerate 
		if (moveForward) {
			accelerate(delta, transform, movement, vehicle);
		}
		
		//apply thrust left
		if (moveLeft) {
			accelLeft(delta, transform, movement, vehicle);
		}
		
		//apply thrust right
		if (moveRight) {
			accelRight(delta, transform, movement, vehicle);
		}
		
		//stop vehicle
		if (applyBreaks) {					
			decelerate(delta, movement);
		}
		
		//fire cannon / attack
		if (shoot) {							
			fireCannon(transform, movement, cannon, Mappers.vehicle.get(vehicleEntity).id);
		}
		
		//debug force insta-stop
		if (stop) {
			movement.velocity.set(0,0);
			stop = false;
		}
			
	}

	/**
	 * Slow down ship. When ship is slow enough, ship will stop completely
	 * @param delta
	 * @param movement
	 */
	private static void decelerate(float delta, MovementComponent movement) {
		int stopThreshold = 20; 
		int minBreakingOffset = 100;
		int maxBreakingThrust = 1000;
		if (movement.velocity.len() <= stopThreshold) {
			//completely stop if moving really slowly
			movement.velocity.set(0,0);
		} else {
			//thrust amount to slow down by
			float thrust = minBreakingOffset + movement.velocity.len();		
			if (thrust > maxBreakingThrust) {
				thrust = maxBreakingThrust; //cap the braking power
			}
			//add thrust opposite direction of velocity to slow down ship
			float angle = movement.velocity.angle();
			float dx = (float) Math.cos(angle) * thrust * delta;
			float dy = (float) Math.sin(angle) * thrust * delta;
			movement.velocity.add(dx, dy);
		}
	}

	/**
	 * Move ship to the right. TODO: change this to dodge mechanic.
	 * @param delta
	 * @param transform
	 * @param movement
	 * @param vehicle
	 */
	private void accelRight(float delta, TransformComponent transform, MovementComponent movement, VehicleComponent vehicle) {
		float thrust = vehicle.thrust * 0.6f;
		float angle = transform.rotation - 1.57f;
		float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
		float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
		movement.velocity.add(dx, dy);
		if (vehicle.maxSpeed != -1)
			movement.velocity.clamp(0, vehicle.maxSpeed);
	}

	/**
	 * Move ship to the left. TODO: change this to dodge mechanic.
	 * @param delta
	 * @param transform
	 * @param movement
	 * @param vehicle
	 */
	private void accelLeft(float delta, TransformComponent transform, MovementComponent movement, VehicleComponent vehicle) {
		float thrust = vehicle.thrust * 0.6f;
		float angle = transform.rotation + 1.57f;
		float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
		float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
		movement.velocity.add(dx, dy);
		if (vehicle.maxSpeed != -1)
			movement.velocity.clamp(0, vehicle.maxSpeed);
	}

	/**
	 * Move ship forward.
	 * @param delta
	 * @param transform
	 * @param movement
	 * @param vehicle
	 */
	private void accelerate(float delta, TransformComponent transform, MovementComponent movement, VehicleComponent vehicle) {
		//TODO: create a vector method for the dx = cos... dy = sin... It's used multiple times in the program(movement, missiles..)
		//TODO: implement rest of engine behavior
		//float maxSpeedMultiplier? on android touch controls make maxSpeed be relative to finger distance so that finger distance determines how fast to go			
	
		float thrust = vehicle.thrust;
		float angle = transform.rotation;
		float dx = (float) Math.cos(angle) * (thrust * movementMultiplier) * delta;
		float dy = (float) Math.sin(angle) * (thrust * movementMultiplier) * delta;
		movement.velocity.add(dx, dy);
		
		//cap speed at max. if maxSpeed set to -1 it's infinite(no cap)
		if (vehicle.maxSpeed != -1)
			movement.velocity.clamp(0, vehicle.maxSpeed);
	}

	/**
	 * Refill ammo for the cannon
	 * @param cannon
	 * @param delta 
	 */
	private static void refillAmmo(CannonComponent cannon, float delta) {
		//TODO: cannon refill logic needs to be moved to system, all ships need to recharge
		// deal with cannon timers
		cannon.timeSinceLastShot -= 100 * delta;
		cannon.timeSinceRecharge -= 100 * delta;
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
		//check if can fire before shooting
		if (!canFire(cannon))
			return;
		
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
	private static boolean canFire(CannonComponent cannon) {
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
		BoundsComponent playerBounds = Mappers.bounds.get(playerEntity);
		for (Entity vehicle : vehicles) {
			BoundsComponent vehicleBounds = Mappers.bounds.get(vehicle);			
			
			//check if character is near a vehicle TODO: check if vehicle empty/available
			if (playerBounds.poly.getBoundingRectangle().overlaps(vehicleBounds.poly.getBoundingRectangle())) {			
				if (Intersector.overlapConvexPolygons(vehicleBounds.poly, playerBounds.poly)) {
					
					//TODO: find better way to do this, check entity for vehicle component?
					//Change vehicle component to save the controlling entity(the driver). reference null if vehicle empty.
					//generic to work with AI and player (and in theory arbitrary entities if necessary)
					vehicleEntity = vehicle; //set vehicle reference

					//zoom out camera
					engine.getSystem(SpaceRenderingSystem.class).setZoomTarget(1);
					//TODO add animation to slowly move focus to the vehicle instead of instantly jumping to the vehicle position
					//engine.getSystem(RenderingSystem.class).pan(vehicleTransform);
					
					
					//TODO make switch focus method? (oldEntity, newEntity)
					//TODO there is a crash here when entering vehicle sometimes...find it, fix it.
					//set focus to vehicle
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
		engine.getSystem(SpaceRenderingSystem.class).setZoomTarget(0.4f);
		
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
