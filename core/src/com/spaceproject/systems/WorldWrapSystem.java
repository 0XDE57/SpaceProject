package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class WorldWrapSystem extends IteratingSystem {

	int wrap;
	
	public WorldWrapSystem(int tileSize, int mapSize) {
		super(Family.all(TransformComponent.class).get());
		
		wrap = tileSize * mapSize;
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		Vector3 pos = Mappers.transform.get(entity).pos;
		if (pos.x >= wrap) pos.x %= wrap;
		if (pos.y >= wrap) pos.y %= wrap;
		if (pos.x < 0) pos.x += wrap;
		if (pos.y < 0) pos.y += wrap;
	}

}
