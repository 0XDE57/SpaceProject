package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class MovementSystem extends IteratingSystem {
	
	//temporary 
	private Vector2 tmp = new Vector2(); 
	
	public MovementSystem() {
		super(Family.all(TransformComponent.class, MovementComponent.class).get());
	}

	@Override
	public void processEntity(Entity entity, float deltaTime) {
		TransformComponent position = Mappers.transform.get(entity);
		MovementComponent movement = Mappers.movement.get(entity);
		
		//set velocity
		tmp.set(movement.accel).scl(deltaTime);
		movement.velocity.add(tmp);
		
		//add velocity to position
		tmp.set(movement.velocity).scl(deltaTime);
		position.pos.add(tmp.x, tmp.y, 0.0f);
	}
}
