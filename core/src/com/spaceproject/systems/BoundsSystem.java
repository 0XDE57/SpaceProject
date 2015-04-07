package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.TransformComponent;

public class BoundsSystem extends IteratingSystem {

	private ComponentMapper<TransformComponent> transformMap;
	private ComponentMapper<BoundsComponent> boundsMap;
	
	@SuppressWarnings("unchecked")
	public BoundsSystem() {
		super(Family.all(BoundsComponent.class, TransformComponent.class).get());
		transformMap = ComponentMapper.getFor(TransformComponent.class);
		boundsMap = ComponentMapper.getFor(BoundsComponent.class);
		
	}

	@Override
	public void processEntity(Entity entity, float deltaTime) {
		TransformComponent transform = transformMap.get(entity);
		BoundsComponent bounds = boundsMap.get(entity);
		
		//center bounding box on entity position
		bounds.bounds.x = transform.pos.x - bounds.bounds.width * 0.5f;
		bounds.bounds.y = transform.pos.y - bounds.bounds.height * 0.5f;
		
	}

}
