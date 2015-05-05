package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;

public class OrbitSystem extends IteratingSystem {

	private ComponentMapper<OrbitComponent> orbitMap;
	private ComponentMapper<TransformComponent> transformMap;

	@SuppressWarnings("unchecked")
	public OrbitSystem() {
		super(Family.all(OrbitComponent.class, TransformComponent.class).get());

		orbitMap = ComponentMapper.getFor(OrbitComponent.class);
		transformMap = ComponentMapper.getFor(TransformComponent.class);
	}

	@Override
	protected void processEntity(Entity entity, float delta) {

		OrbitComponent orbit = orbitMap.get(entity);
		TransformComponent position = transformMap.get(entity);
		TransformComponent parentPosition = transformMap.get(orbit.parent);

		if (orbit.rotateClockwise) {
			// add clockwise rotation to entity image
			position.rotation += orbit.rotSpeed * delta;

			// add clockwise rotation to entity orbit
			orbit.angle += orbit.orbitSpeed * delta;
		} else {
			// add counter-clockwise rotation to entity image
			position.rotation -= orbit.rotSpeed * delta;

			// add counter-clockwise rotation to entity orbit
			orbit.angle -= orbit.orbitSpeed * delta;
		}

		// calculate orbit position
		float orbitX = parentPosition.pos.x + (orbit.distance * MathUtils.cos(orbit.angle));
		float orbitY = parentPosition.pos.y + (orbit.distance * MathUtils.sin(orbit.angle));
		position.pos.set(orbitX, orbitY, position.pos.z);

	}

}
