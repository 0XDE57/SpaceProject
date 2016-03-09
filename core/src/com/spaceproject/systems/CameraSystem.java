package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class CameraSystem extends IteratingSystem {
	
	private static OrthographicCamera cam;
	
	public CameraSystem(OrthographicCamera camera) {
		super(Family.all(PlayerFocusComponent.class).get());
		cam = camera;
	}
	
	public void processEntity(Entity entity, float delta) {
		TransformComponent transform = Mappers.transform.get(entity);
		
		//set camera position to entity
		cam.position.x = transform.pos.x;
		cam.position.y = transform.pos.y;
	}	

}
