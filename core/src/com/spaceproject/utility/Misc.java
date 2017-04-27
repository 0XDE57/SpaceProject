package com.spaceproject.utility;

import java.lang.reflect.Field;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;

/** TODO: Misc is a terrible class name */
public class Misc {
	
	public static Entity copyEntity(Entity entity) {
		Entity newEntity = new Entity();		
		for (Component c : entity.getComponents()) {
			//TODO: broken...also this is stupid, 
			//clone,
			//https://stackoverflow.com/questions/869033/how-do-i-copy-an-object-in-java
			/*
			Component copy = new (instanceof c)
			for (Field f : c.getClass().getFields()) {
				
			}*/
			newEntity.add(c);
		}
		return newEntity;
	}
	
	public static Entity closestEntity(Vector3 position, ImmutableArray<Entity> entities) {
		if (entities == null || entities.size() == 0)
			return null;
		
		Entity targetEntity = entities.first();
		float targetDist = MyMath.distance(position, Mappers.transform.get(targetEntity).pos);
		for (Entity searchEnt : entities) {
			float dist = MyMath.distance(Mappers.transform.get(searchEnt).pos, position);
			if (dist < targetDist) {
				targetDist = dist;
				targetEntity = searchEnt;
			}
		}

		return targetEntity;
	}
}
