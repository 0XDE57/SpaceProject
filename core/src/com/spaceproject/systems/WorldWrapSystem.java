package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyScreenAdapter;

public class WorldWrapSystem extends EntitySystem {
	/** TODO: fix world wrapping
	 *
	 * The world/tiles are simply wrapped using techniques described here:
	 * http://ronvalstar.nl/creating-tileable-noise-maps/
	 * https://gamedev.stackexchange.com/questions/23625/how-do-you-generate-tileable-perlin-noise
	 *
	 *
	 * But the physics and rendering do not like this and there are some technical difficulties to overcome:
	 * -“Physics and collision detection have to deal with fact, that object,
	 * that crossing the border can be in two (or even four) places
	 * simultaneously, and have to collide with other objects from other side.”
	 * -“Ai and pathfinding (Bots should see through the border).”
	 * “Sounds should be properly positioned.”
	 * -“Effects and trails. Just imagine particle emitter crossing the
	 * border, and what you should do to make it properly visible from both
	 * sides?”
	 * https://simonschreibt.de/gat/1nsane-carpet-2-repetitive-worlds/
	 *
	 */

	int wrap;
	int offsetX, offsetY;
	
	ImmutableArray<Entity> entities;
	
	public WorldWrapSystem(int mapSize) {
		wrap = SpaceProject.tileSize * mapSize;
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

			//simple wrap approach
			if (entityPos.x >= wrap) entityPos.x -= wrap;
			if (entityPos.y >= wrap) entityPos.y -= wrap;
			if (entityPos.x < 0) entityPos.x += wrap;
			if (entityPos.y < 0) entityPos.y += wrap;

			/*
			//simple offset approach
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
