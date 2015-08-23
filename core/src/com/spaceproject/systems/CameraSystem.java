package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TransformComponent;

public class CameraSystem extends IteratingSystem {
	
	private ComponentMapper<TransformComponent> transformMap;
	
	public CameraSystem() {
		super(Family.all(PlayerFocusComponent.class).get());
		
		transformMap = ComponentMapper.getFor(TransformComponent.class);

	}
	
	public void processEntity(Entity entity, float delta) {
		TransformComponent transform = transformMap.get(entity);
		
		//set camera position to entity
		RenderingSystem.getCam().position.x = transform.pos.x;
		RenderingSystem.getCam().position.y = transform.pos.y;
	}	

}
