package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class BoundsSystem extends IteratingSystem {
	
	//TODO: probably combine this with collision system, why is this separate, its part of the same thing
	public BoundsSystem() {
		super(Family.all(BoundsComponent.class, TransformComponent.class).get());
		
	}

	@Override
	public void processEntity(Entity entity, float deltaTime) {
		TransformComponent transform = Mappers.transform.get(entity);
		BoundsComponent bounds = Mappers.bounds.get(entity);
		
		//center bounding box on entity position		
		bounds.poly.setPosition(transform.pos.x - bounds.poly.getOriginX(), transform.pos.y - bounds.poly.getOriginY());
		bounds.poly.setRotation(transform.rotation * MathUtils.radiansToDegrees);		
		
	}

}
