package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.TransformComponent;

public class MovementSystem extends IteratingSystem {

	private ComponentMapper<TransformComponent> transfromMap;
	private ComponentMapper<MovementComponent> movementMap;
	
	//temporary 
	private Vector2 tmp = new Vector2(); 

	
	@SuppressWarnings("unchecked")
	public MovementSystem() {
		super(Family.all(TransformComponent.class, MovementComponent.class).get());
		
		transfromMap = ComponentMapper.getFor(TransformComponent.class);
		movementMap = ComponentMapper.getFor(MovementComponent.class);
	}

	@Override
	public void processEntity(Entity entity, float deltaTime) {
		TransformComponent position = transfromMap.get(entity);
		MovementComponent movement = movementMap.get(entity);;
		
		//set velocity
		tmp.set(movement.accel).scl(deltaTime);
		movement.velocity.add(tmp);
		
		//add velocity to position
		tmp.set(movement.velocity).scl(deltaTime);
		position.pos.add(tmp.x, tmp.y, 0.0f);
		
	}
}
