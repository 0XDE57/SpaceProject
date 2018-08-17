package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class OrbitSystem extends IteratingSystem {

	public OrbitSystem() {
		super(Family.all(OrbitComponent.class, TransformComponent.class).get());
	}

	@Override
	protected void processEntity(Entity entity, float delta) {
		OrbitComponent orbit = Mappers.orbit.get(entity);
		TransformComponent position = Mappers.transform.get(entity);


		//TODO: time sync planet rotation/spin just like orbit
		position.rotation += orbit.rotateClockwise ? orbit.rotSpeed * delta : -orbit.rotSpeed * delta;

		orbit.angle = getTimeSyncAngle(orbit, GameScreen.gameTimeCurrent);

		if (orbit.parent != null) {
			//apply tangential velocity
			position.velocity.set(orbit.tangentialSpeed, 0).rotateRad(orbit.angle).rotate90(orbit.rotateClockwise ? 1 : -1);

			// calculate exact orbit position
			Vector2 orbitPos = getSyncPos(entity, GameScreen.gameTimeCurrent);
			//ensure object is not too far from synced location
			if (!position.pos.epsilonEquals(orbitPos, 10)) {
				position.pos.set(orbitPos);
			}
		}
	}

	public static Vector2 getSyncPos(Entity entity, long time) {
		OrbitComponent orbit = Mappers.orbit.get(entity);
		TransformComponent parentPosition = Mappers.transform.get(orbit.parent);
		return MyMath.Vector(getTimeSyncAngle(orbit, time), orbit.radialDistance).add(parentPosition.pos);
	}


	public static float getTimeSyncAngle(OrbitComponent orbit, long gameTime) {
		//calculate time-synced angle, dictate position as a function of time based on tangential velocity
		float angularSpeed = orbit.tangentialSpeed / orbit.radialDistance;
		long msPerRevolution = (long)(1000 * MathUtils.PI2 / angularSpeed);
		float timeSyncAngle = 0;
		if (msPerRevolution != 0) {
			timeSyncAngle = MathUtils.PI2 * ((float)(gameTime % msPerRevolution) / (float)msPerRevolution);
		}

		timeSyncAngle = orbit.rotateClockwise ? timeSyncAngle : -timeSyncAngle;

		//keep angle relative to starting position
		timeSyncAngle += orbit.startAngle;

		//keep angle within 0 to 2PI radians
		if (timeSyncAngle > MathUtils.PI2){
			timeSyncAngle -= MathUtils.PI2;
		} else if (timeSyncAngle < 0) {
			timeSyncAngle += MathUtils.PI2;
		}

		return timeSyncAngle;
	}

}
