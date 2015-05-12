package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.spaceproject.components.TransformComponent;

public class CameraSystem extends EntitySystem {
	
	private Entity target;
	private ComponentMapper<TransformComponent> transformMap;
	
	public CameraSystem(Entity target) {
		transformMap = ComponentMapper.getFor(TransformComponent.class);
		
		this.target = target;
	}
	
	public void update(float delta) {
		TransformComponent transform = transformMap.get(target);
		
		//set camera position to entity
		RenderingSystem.getCam().position.x = transform.pos.x;
		RenderingSystem.getCam().position.y = transform.pos.y;
	}	

	//get target camera is focused on
	public Entity getTarget() {
		return target;
	}

	//set target entity to focus camera on
	public void setTarget(Entity target) {
		this.target = target;
	}


}
