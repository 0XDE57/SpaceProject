package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class OrbitSystem extends IteratingSystem {

	public OrbitSystem() {
		super(Family.all(OrbitComponent.class, TransformComponent.class).get());
	}


	@Override
	protected void processEntity(Entity entity, float delta) {
		//TODO: take off system will have to go from planet ID/seed instead of saved x,y pos

		OrbitComponent orbit = Mappers.orbit.get(entity);
		TransformComponent position = Mappers.transform.get(entity);		



		//goal: tangential velocity (between generated min and max) to dictate position as a function of time

		//ω = (2pi\T)= 2pi*f
		//ω is the angular frequency or angular speed (measured in radians per second),
		//T is the period (measured in seconds), duration of time of one cycle. or in orbital mechanics the mean motion
		//f is the ordinary frequency (measured in hertz)

		//T = 2pi/W
		//f = 1/T

		//tangential velocity: the linear speed of something moving along a circular path. the velocity is
		//directly proportional to rotational speed at any fixed radialDistance from the axis of rotation.
		//When proper units are used for tangential speed v, rotational speed ω, and radial radialDistance r, the direct proportion of v to both r and ω becomes the exact equation
		//v = rω
		//ω = v/r


		//calculate current angle
		if (orbit.rotateClockwise) {
			// add clockwise rotation to entity image and orbit
			position.rotation += orbit.rotSpeed * delta;
			if (orbit.msPerRevolution != 0) {
				orbit.angle = MathUtils.PI2 * ((double)(GameScreen.gameTimeCurrent % orbit.msPerRevolution) / (double)orbit.msPerRevolution);
			}
		} else {
			// add counter-clockwise rotation to entity image and orbit
			position.rotation -= orbit.rotSpeed * delta;
			if (orbit.msPerRevolution != 0) {
				orbit.angle = -MathUtils.PI2 * ((double) (GameScreen.gameTimeCurrent % orbit.msPerRevolution) / (double) orbit.msPerRevolution);
			}
		}

		//keep angle relative to starting position
		orbit.angle += orbit.startAngle;

		//keep angles within 0 to 2PI radians
		if (orbit.angle > MathUtils.PI2){
			orbit.angle -= MathUtils.PI2;
		} else if (orbit.angle < 0) {
			orbit.angle += MathUtils.PI2;
		}

		if (orbit.parent != null) {

			// calculate orbit position
			TransformComponent parentPosition = Mappers.transform.get(orbit.parent);
			double orbitX = parentPosition.pos.x + ((double)orbit.radialDistance * MathUtils.cos((float)orbit.angle));
			double orbitY = parentPosition.pos.y + ((double)orbit.radialDistance * MathUtils.sin((float)orbit.angle));
			Vector3 realPos = new Vector3((float)orbitX, (float)orbitY, position.pos.z);

			//f = 1/T = 1 / 1256.63 = 0.000795
			//ω = 2pi*f = 2pi* 0.000795 = 0.005 = radians per second
			//v = rω = 2000*0.005 = 10
			float angularVelocity = MathUtils.PI2/orbit.msPerRevolution*1000;
			float vel = orbit.radialDistance * angularVelocity;
			position.velocity.set(vel, 0);
			position.velocity.rotateRad((float)orbit.angle);
			position.velocity.rotate90(orbit.rotateClockwise ? 1 : -1);


			//ensure we are not too far from actual desired location
			if (!position.pos.epsilonEquals(realPos, 10)) {
				position.pos.set(realPos);
			}

		}
	}
	boolean synced = false;
}
