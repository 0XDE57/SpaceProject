package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class OrbitSystem extends IteratingSystem {

	public OrbitSystem() {
		super(Family.all(OrbitComponent.class, TransformComponent.class).get());
	}

	@Override
	protected void processEntity(Entity entity, float delta) {
		//TODO: switch to time based orbit so planets still "move" while on a world.
		//take off system will have to go from planet ID/seed instead of saved x,y pos (this broken already)

		OrbitComponent orbit = Mappers.orbit.get(entity);
		TransformComponent position = Mappers.transform.get(entity);		

		//keep angles within 0 to 2PI radians
		if (orbit.angle > MathUtils.PI2){
			orbit.angle -= MathUtils.PI2;
		} else if (orbit.angle < 0) {
			orbit.angle += MathUtils.PI2;
		}
		
		if (orbit.rotateClockwise) {
			// add clockwise rotation to entity image and orbit
			position.rotation += orbit.rotSpeed * delta;
			orbit.angle += orbit.orbitSpeed * delta;
		} else {
			// add counter-clockwise rotation to entity image and orbit
			position.rotation -= orbit.rotSpeed * delta;
			orbit.angle -= orbit.orbitSpeed * delta;
		}


		if (orbit.parent != null) {
			TransformComponent parentPosition = Mappers.transform.get(orbit.parent);

			// calculate orbit position
			float orbitX = parentPosition.pos.x + (orbit.distance * MathUtils.cos(orbit.angle));
			float orbitY = parentPosition.pos.y + (orbit.distance * MathUtils.sin(orbit.angle));
			Vector3 nextPos = new Vector3(orbitX, orbitY, position.pos.z);
			// linear interpolate to smooth out movement and eliminate "jumping" visible on long orbit distances.
			position.pos.lerp(nextPos, 1f*delta);
		}
	}

}
