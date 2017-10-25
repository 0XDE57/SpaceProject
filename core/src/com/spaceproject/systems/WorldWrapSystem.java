package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyScreenAdapter;

public class WorldWrapSystem extends EntitySystem {

	int wrap;
	int offsetX, offsetY;
	
	ImmutableArray<Entity> entities;
	
	public WorldWrapSystem(int tileSize, int mapSize) {		
		wrap = tileSize * mapSize;
	}
	
	@Override
	public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
		entities = engine.getEntitiesFor(Family.all(TransformComponent.class).get());
	}

	@Override
	public void update(float delta) {
		boolean shifted = false;
		Vector3 camPos = MyScreenAdapter.cam.position;
		//if cam away from border and crossed border, shift...?
		
		
		for (Entity entity : entities) {
			Vector3 entityPos = Mappers.transform.get(entity).pos;

			
			if (entityPos.x >= wrap) entityPos.x -= wrap;
			if (entityPos.y >= wrap) entityPos.y -= wrap;
			if (entityPos.x < 0) entityPos.x += wrap;
			if (entityPos.y < 0) entityPos.y += wrap;
			/*
			if (entityPos.x >= wrap) {
				entityPos.x -= wrap;
				offsetX += wrap;
				shifted = true;
			}
			if (entityPos.y >= wrap) {
				entityPos.y -= wrap;
				offsetY += wrap;
				shifted = true;
			}
			if (entityPos.x < 0) {
				entityPos.x += wrap;
				offsetX -= wrap;
				shifted = true;
			}
			if (entityPos.y < 0) {
				entityPos.y += wrap;
				offsetY -= wrap;
				shifted = true;
			}*/
		}
		
		if (shifted) {
			System.out.println("Wrap offset: " + offsetX + ", " + offsetY);
		}
	}

}
