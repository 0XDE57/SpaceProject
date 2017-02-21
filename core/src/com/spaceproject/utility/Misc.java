package com.spaceproject.utility;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

/** TODO: Misc is a terrible class name */
public class Misc {
	
	public static Entity copyEntity(Entity entity) {
		Entity newEntity = new Entity();		
		for (Component c : entity.getComponents()) {
			newEntity.add(c);
		}
		return newEntity;
	}
}
