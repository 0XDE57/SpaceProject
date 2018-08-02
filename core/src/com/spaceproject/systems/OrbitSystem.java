package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;

public class OrbitSystem extends IteratingSystem implements EntityListener {

	public OrbitSystem() {
		super(Family.all(OrbitComponent.class, TransformComponent.class).get());
	}

	@Override
	public void addedToEngine (Engine engine) {
		Family test = Family.all(ControlFocusComponent.class).get();
		engine.addEntityListener(test,this);

		super.addedToEngine(engine);

	}

	@Override
	protected void processEntity(Entity entity, float delta) {
		//TODO: time sync planet rotation/spin just like orbit
		//TODO: take off system will have to go from planet ID/seed instead of saved x,y pos

		OrbitComponent orbit = Mappers.orbit.get(entity);
		TransformComponent position = Mappers.transform.get(entity);

		//calculate time-synced angle, dictate position as a function of time based on tangential velocity
		float angularSpeed = orbit.tangentialSpeed / orbit.radialDistance;
		long msPerRevolution = (long)(1000 * MathUtils.PI2 / angularSpeed);
		double timeSyncAngle = 0;
		if (msPerRevolution != 0) {
			timeSyncAngle = MathUtils.PI2 * ((double)(GameScreen.gameTimeCurrent % msPerRevolution) / (double)msPerRevolution);
		}


		if (orbit.rotateClockwise) {
			// add clockwise rotation to entity image and orbit
			position.rotation += orbit.rotSpeed * delta;
			orbit.angle = timeSyncAngle;
		} else {
			// add counter-clockwise rotation to entity image and orbit
			position.rotation -= orbit.rotSpeed * delta;
			orbit.angle = -timeSyncAngle;
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
			//apply tangential velocity
			position.velocity.set(orbit.tangentialSpeed, 0);
			position.velocity.rotateRad((float)orbit.angle);
			position.velocity.rotate90(orbit.rotateClockwise ? 1 : -1);

			// calculate exact orbit position
			TransformComponent parentPosition = Mappers.transform.get(orbit.parent);
			double orbitX = parentPosition.pos.x + ((double)orbit.radialDistance * MathUtils.cos((float)orbit.angle));
			double orbitY = parentPosition.pos.y + ((double)orbit.radialDistance * MathUtils.sin((float)orbit.angle));
			Vector3 orbitPos = new Vector3((float)orbitX, (float)orbitY, position.pos.z);

			//ensure object is not too far from synced location
			if (!position.pos.epsilonEquals(orbitPos, 10)) {
				position.pos.set(orbitPos);
			}
		}

	}

	@Override
	public void entityAdded(Entity entity) {
		System.out.println("entityAdded++++++++++++++++++++++++++++++++++++");
		Misc.printEntity(entity);
	}

	@Override
	public void entityRemoved(Entity entity) {
		System.out.println("entityRemoved----------------------------------");
		//Misc.printEntity(entity);
	}
}
