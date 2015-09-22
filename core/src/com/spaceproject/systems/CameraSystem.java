package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class CameraSystem extends IteratingSystem {
	
	public CameraSystem() {
		super(Family.all(PlayerFocusComponent.class).get());

	}
	
	public void processEntity(Entity entity, float delta) {
		TransformComponent transform = Mappers.transform.get(entity);
		
		//set camera position to entity
		RenderingSystem.getCam().position.x = transform.pos.x;
		RenderingSystem.getCam().position.y = transform.pos.y;
	}	

}
