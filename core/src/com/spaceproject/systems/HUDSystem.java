package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;

public class HUDSystem  extends EntitySystem {
	
	private ShapeRenderer shape;
	private Engine engine;
	
	private ComponentMapper<TransformComponent> transMap;
	private ComponentMapper<MapComponent> mapMap;
	
	private ImmutableArray<Entity> mapableObjects; //planets, stars
	
	public HUDSystem() {
		transMap = ComponentMapper.getFor(TransformComponent.class);
		mapMap = ComponentMapper.getFor(MapComponent.class);
		
		shape = new ShapeRenderer();
	}
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;
		
		mapableObjects = engine.getEntitiesFor(Family.all(MapComponent.class).get());
		
	}
	
	@Override
	public void update(float delta) {
		//Vector3 camPos = RenderingSystem.getCam().position;
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();
		
		int centerX = width/2;
		int centerY = height/2;
		
		shape.begin(ShapeType.Filled);
		shape.setColor(1, 1, 1, 1);
		for (Entity mapable : mapableObjects) {
			TransformComponent trans = transMap.get(mapable);
			Vector3 screenPos = RenderingSystem.getCam().project(trans.pos.cpy());
			
			if (screenPos.x > 0 && screenPos.x < width && screenPos.y > 0 && screenPos.y < height) {
				continue;
			}
			
			float mapX = 0, mapY = 0;
			if (screenPos.y > centerY) {
				//top
				mapX = (screenPos.x-centerX) * (centerY / (screenPos.y - centerY)) + centerX;
				mapY = height;
				if (mapX < 0) {
					//left
					mapX = 0;
					mapY = height-((screenPos.y-centerY) * (centerX / (screenPos.x - centerX)) + centerY);
				} else if (mapX > width) {
					//right
					mapX = width;
					mapY = (screenPos.y-centerY) * (centerX / (screenPos.x - centerX)) + centerY;
				}				
			} else {
				//bottom
				mapX = width-((screenPos.x-centerX) * (centerY / (screenPos.y - centerY)) + centerX);
				mapY = 0;
				if (mapX < 0) {
					//left
					mapX = 0;
					mapY = height-((screenPos.y-centerY) * (centerX / (screenPos.x - centerX)) + centerY);
				} else if (mapX > width) {
					//right
					mapX = width;
					mapY = (screenPos.y-centerY) * (centerX / (screenPos.x - centerX)) + centerY;
				}
			}
			
			shape.circle(mapX, mapY, 8);

		}
		
		shape.end();
	}
	

}
