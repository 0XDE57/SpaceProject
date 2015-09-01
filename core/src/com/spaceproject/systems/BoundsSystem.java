package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
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
		bounds.poly.setPosition(transform.pos.x - bounds.poly.getOriginX(), transform.pos.y - bounds.poly.getOriginY());
		bounds.poly.setRotation(transform.rotation * MathUtils.radiansToDegrees);		
		
	}

}
