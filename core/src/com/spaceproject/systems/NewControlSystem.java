package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.LandConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.SpaceScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyIteratingSystem;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;

public class NewControlSystem extends MyIteratingSystem {

	private ImmutableArray<Entity> vehicles;
	private ImmutableArray<Entity> planets;
	private boolean inSpace;
	
	public NewControlSystem(MyScreenAdapter screen, Engine engine) {
		super(Family.all(ControllableComponent.class, TransformComponent.class).one(
				CharacterComponent.class, VehicleComponent.class).get());
		
		vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
		planets = engine.getEntitiesFor(Family.all(PlanetComponent.class).get());
		
		inSpace = (screen instanceof SpaceScreen);
	}

	@Override
	protected void processEntity(Entity entity, float delta) {
		
		ControllableComponent control = Mappers.controllable.get(entity);
		
		CharacterComponent character = Mappers.character.get(entity);
		if (character != null) {
			controlCharacter(entity, character, control, delta);
		}
		
		VehicleComponent vehicle = Mappers.vehicle.get(entity);
		if (vehicle != null) {
			controlShip(entity, vehicle, control, delta);
		}
		
	}

	private void controlCharacter(Entity entity, CharacterComponent character, ControllableComponent control, float delta) {
		//players position
		TransformComponent transform = Mappers.transform.get(entity);
		
		//make character face mouse/joystick
		transform.rotation = MathUtils.lerpAngle(transform.rotation, control.angleFacing, 8f*delta);
		
		if (control.moveForward) {
			float walkSpeed = character.walkSpeed;
			float dx = (float) Math.cos(transform.rotation) * (walkSpeed * control.movementMultiplier) * delta;
			float dy = (float) Math.sin(transform.rotation) * (walkSpeed * control.movementMultiplier) * delta;
			transform.pos.add(dx, dy, 0);
		}
		
		if (control.changeVehicle) {			
			enterVehicle(entity, control);
		}
		
	}

	private void controlShip(Entity entity, VehicleComponent vehicle, ControllableComponent control, float delta) {
		
		TransformComponent transform = Mappers.transform.get(entity);
		//VehicleComponent vehicle = Mappers.vehicle.get(vehicleEntity);
		
		CannonComponent cannon = Mappers.cannon.get(entity);	
		refillAmmo(cannon, delta);
		
		//make vehicle face angle from mouse/joystick
		transform.rotation = MathUtils.lerpAngle(transform.rotation, control.angleFacing, 8f*delta);
		
		
		//apply thrust forward accelerate 
		if (control.moveForward) {
			accelerate(delta, control, transform, vehicle);
		}
		
		//apply thrust left
		if (control.moveLeft) {
			dodgeLeft();
		}
		
		//apply thrust right
		if (control.moveRight) {
			dodgeRight();
		}
		
		//stop vehicle
		if (control.moveBack) {
			decelerate(delta, transform);
		}
		
		//fire cannon / attack
		if (control.shoot) {
			int id = Mappers.vehicle.get(entity).id;
			fireCannon(transform, cannon, id);
		}
		
		//land or take off from planet
		if (control.land) {
			if (inSpace)
				landOnPlanet(entity);
			else
				takeOffPlanet(entity);
		}
		
		//debug force insta-stop
		if (Gdx.input.isKeyJustPressed(Keys.X)) transform.velocity.set(0,0);
		
		//exit vehicle
		if (control.changeVehicle) {
			exitVehicle(entity, control);
		}
			
	}
	
	private static void takeOffPlanet(Entity entity) {
		ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
		screenTrans.stage = ScreenTransitionComponent.AnimStage.transition;//begin animation
		screenTrans.landCFG = new LandConfig();
		//TODO: load location that should be saved from when landed
		
		//screenTrans.landCFG.planet = Mappers.planet.get(planet);//generation properties(seed,size,octave,etc..)
		screenTrans.landCFG.ship = Misc.copyEntity(entity);//entity to send to the planet
		//screenTrans.landCFG.position = WORLDSCREEN.planetPos;// save position for taking off from planet
		
		entity.add(screenTrans);
		
	}

	private void landOnPlanet(Entity entity) {
		Vector3 vePos = Mappers.transform.get(entity).pos;
		for (Entity planet : planets) {
			Vector3 planetPos = Mappers.transform.get(planet).pos;
			TextureComponent planetTex = Mappers.texture.get(planet);

			if (MyMath.distance(vePos.x, vePos.y, planetPos.x, planetPos.y) <= planetTex.texture.getWidth() * 0.5 * planetTex.scale) {				
				ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
				screenTrans.stage = ScreenTransitionComponent.AnimStage.shrink;//begin animation
				screenTrans.landCFG = new LandConfig();
				screenTrans.landCFG.planet = Mappers.planet.get(planet);//generation properties(seed,size,octave,etc..)
				screenTrans.landCFG.ship = Misc.copyEntity(entity);//entity to send to the planet
				screenTrans.landCFG.position = planetPos;// save position for taking off from planet
				//TODO: planet moves, set position to what ever planet is instead (will come into play
				//over more when orbit is based on time)
				entity.add(screenTrans);
				return;
			}
		}
	}
	
	private boolean canLandOnPlanet(Entity vehicleEntity) {
		//Entity vehicleEntity = Mappers.character.get(playerEntity).vehicle;
		Vector3 vePos = Mappers.transform.get(vehicleEntity).pos;
		for (Entity planet : planets) {
			Vector3 planetPos = Mappers.transform.get(planet).pos;
			TextureComponent planetTex = Mappers.texture.get(planet);
			// if player is over planet
			if (MyMath.distance(vePos.x, vePos.y, planetPos.x, planetPos.y) <= planetTex.texture.getWidth() * 0.5 * planetTex.scale) {				
				return true;
			}
		}
		return false;
	}

	/** Slow down ship. When ship is slow enough, ship will stop completely */
	private static void decelerate(float delta, TransformComponent transform) {
		int stopThreshold = 20; 
		int minBreakingThrust = 10;
		int maxBreakingThrust = 1500;
		if (transform.velocity.len() <= stopThreshold) {
			//completely stop if moving really slowly
			transform.velocity.set(0,0);
		} else {
			//add thrust opposite direction of velocity to slow down ship
			float thrust = MathUtils.clamp(transform.velocity.len(), minBreakingThrust, maxBreakingThrust);
			float angle = transform.velocity.angle();
			float dx = (float) Math.cos(angle) * thrust * delta;
			float dy = (float) Math.sin(angle) * thrust * delta;
			transform.velocity.add(dx, dy);
		}
	}

	private static void dodgeRight() {}

	private static void dodgeLeft() {}

	private static void accelerate(float delta, ControllableComponent control, TransformComponent transform, VehicleComponent vehicle) {
		//TODO: create a vector method for the dx = cos... dy = sin... It's used multiple times in the program(movement, missiles..)
		//TODO: implement rest of engine behavior
		//float maxSpeedMultiplier? on android touch controls make maxSpeed be relative to finger distance so that finger distance determines how fast to go			
	
		float thrust = vehicle.thrust;
		float angle = transform.rotation;
		float dx = (float) Math.cos(angle) * (thrust * control.movementMultiplier) * delta;
		float dy = (float) Math.sin(angle) * (thrust * control.movementMultiplier) * delta;
		transform.velocity.add(dx, dy);
		
		//cap speed at max. if maxSpeed set to -1 it's infinite(no cap)
		if (vehicle.maxSpeed != -1)
			transform.velocity.clamp(0, vehicle.maxSpeed);
	}

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

	private void fireCannon(TransformComponent transform, CannonComponent cannon, long ID) {
		//check if can fire before shooting
		if (!canFire(cannon))
			return;
		
		//reset timer if ammo is full, to prevent instant recharge
		if (cannon.curAmmo == cannon.maxAmmo) {			
			cannon.timeSinceRecharge = cannon.rechargeRate;
		}		
		
		//create missile	
		float dx = (float) (Math.cos(transform.rotation) * cannon.velocity) + transform.velocity.x;
		float dy = (float) (Math.sin(transform.rotation) * cannon.velocity) + transform.velocity.y;
		engine.addEntity(EntityFactory.createMissile(transform, new Vector2(dx, dy), cannon, ID));
		
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
	
	
	public void enterVehicle(Entity characterEntity, ControllableComponent control) {
		//check if already in vehicle
		//if (isInVehicle(playerEntity))  return;
		
		//Mappers.character.get(entity)
		
		//action timer
		if (control.timeSinceVehicle < control.timeTillCanGetInVehicle) {
			return;
		}
		control.timeSinceVehicle = 0;
		
		
		//get all vehicles and check if player is close to one(bounds overlap)
		BoundsComponent playerBounds = Mappers.bounds.get(characterEntity);
		for (Entity vehicle : vehicles) {
			
			//skip vehicle is occupied
			if (Mappers.vehicle.get(vehicle).driver != null) continue;
			
			//check if character is near a vehicle
			BoundsComponent vehicleBounds = Mappers.bounds.get(vehicle);
			if (playerBounds.poly.getBoundingRectangle().overlaps(vehicleBounds.poly.getBoundingRectangle())) {			
				
				//set references
				Mappers.character.get(characterEntity).vehicle = vehicle;
				Mappers.vehicle.get(vehicle).driver = characterEntity;
				
				/*
				// set focus to vehicle
				characterEntity.remove(CameraFocusComponent.class);
				vehicle.add(new CameraFocusComponent());
				
				characterEntity.remove(ControllableComponent.class);
				vehicle.add(new ControllableComponent());
				*/
				vehicle.add(characterEntity.remove(CameraFocusComponent.class));
				vehicle.add(characterEntity.remove(ControllableComponent.class));
				
				// remove player from engine
				engine.removeEntity(characterEntity);
				
				//if (entity is controlled by player)
				// zoom out camera, TODO: add pan animation
				MyScreenAdapter.setZoomTarget(1);
				
				return;
			}
		}
	}
			

	public void exitVehicle(Entity vehicleEntity, ControllableComponent control) {
		
		//action timer
		if (control.timeSinceVehicle < control.timeTillCanGetInVehicle) {
			return;
		}
		control.timeSinceVehicle = 0;

		Entity characterEntity = Mappers.vehicle.get(vehicleEntity).driver;
		
		// set the player at the position of vehicle
		Vector3 vehiclePosition = Mappers.transform.get(vehicleEntity).pos;
		Mappers.transform.get(characterEntity).pos.set(vehiclePosition);
		
		/*
		// set focus to player entity
		vehicleEntity.remove(CameraFocusComponent.class);
		characterEntity.add(new CameraFocusComponent());
		
		vehicleEntity.remove(ControllableComponent.class);
		characterEntity.add(new ControllableComponent());
		*/
		characterEntity.add(vehicleEntity.remove(CameraFocusComponent.class));
		characterEntity.add(vehicleEntity.remove(ControllableComponent.class));
		
		// remove references
		Mappers.character.get(characterEntity).vehicle = null;
		Mappers.vehicle.get(vehicleEntity).driver = null;
		
		// add player back into world
		engine.addEntity(characterEntity);
		
		// zoom in camera
		MyScreenAdapter.setZoomTarget(0.5f);
		
	}

	/*
	/** Check if player is in vehicle. 
	public static boolean isInVehicle(Entity character) {
		return Mappers.character.get(character).vehicle != null;
	}*/
}
