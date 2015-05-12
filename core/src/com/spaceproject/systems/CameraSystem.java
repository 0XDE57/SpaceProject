package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.TransformComponent;

public class CameraSystem extends EntitySystem {
	
	//private Engine engine;
	private Entity target;
	private ComponentMapper<TransformComponent> transformMap;
	private ImmutableArray<Entity> entities;
	
	static int tileSize = 1000;
	static int localTileX;
	static int localTileY;
	
	public CameraSystem(Entity target) {
		transformMap = ComponentMapper.getFor(TransformComponent.class);
		
		this.target = target;
		
		//localTileX = transformMap.get(target).tileX;
		//localTileX = transformMap.get(target).tileY;
		localTileX = target.getComponent(TransformComponent.class).tileX;
		localTileY = target.getComponent(TransformComponent.class).tileY;
		System.out.println("Local tile: " + localTileX + "," + localTileY);
	}
	
	public static Vector3 getLocalPosition(TransformComponent transform) {
		int locTileX = transform.tileX - localTileX;
		int locTileY = transform.tileY - localTileY;
		return new Vector3(transform.pos.x + (locTileX * tileSize), transform.pos.y + (locTileY * tileSize), transform.pos.z);
	}
	
	public void update(float delta) {
		TransformComponent transform = transformMap.get(target);
		/*
		if (transform.pos.x < 0) {			
			localTileX--;
			
			transform.tileX--;
			transform.pos.x = tileSize;
		} else if (transform.pos.x > tileSize) {
			localTileX++;
		}
		if (transform.pos.y < 0) {
			localTileY--;
		} else if (transform.pos.y > tileSize) {
			localTileY++;
		}*/
		
		if (transform.pos.x < 0) {
			localTileX--;
			for (Entity e : entities) {
				TransformComponent t = transformMap.get(e);
				System.out.println("XO - " + t.tileX + ":" + t.pos.x);//old

				t.tileX += ((int)t.pos.x / tileSize);
				if (t.pos.x < 0) {
					t.tileX--;
				}
				
				float newX = t.pos.x % tileSize;
				if (newX < 0) {
					newX = tileSize + newX;
				}
				t.pos.x = newX;
			}
			System.out.println("Local world: " + localTileX + "," + localTileY);
		} else if (transform.pos.x > tileSize) {
			localTileX++;
			for (Entity e : entities) {
				TransformComponent t = transformMap.get(e);
				System.out.println("XO - " + t.tileX + ":" + t.pos.x);//old							
				t.tileX = t.tileX + (int)(t.pos.x / tileSize);
				
				float newX = t.pos.x % tileSize;
				if (newX < 0) {
					newX = tileSize + newX;
				}
				t.pos.x = newX;
				
				System.out.println("XN - " + t.tileX + ":" + t.pos.x);//new
			}
			System.out.println("Local world: " + localTileX + "," + localTileY);
		}
		if (transform.pos.y < 0) {
			localTileY--;
			for (Entity e : entities) {
				TransformComponent t = transformMap.get(e);
				System.out.println("YO - " + t.tileY + ":" + t.pos.y);//old
				
				t.tileY += ((int)t.pos.y / tileSize);
				if (t.pos.y < 0) {
					t.tileY--;
				}
				
				float newY = t.pos.y % tileSize;
				if (newY < 0) {
					newY = tileSize + newY;
				}
				t.pos.y = newY;
				System.out.println("YN - " + t.tileY + ":" + t.pos.y);//new
			}
			System.out.println("Local world: " + localTileX + "," + localTileY);
		} else if (transform.pos.y > tileSize) {
			localTileY++;
			for (Entity e : entities) {
				TransformComponent t = transformMap.get(e);
				System.out.println("YO - " + t.tileY + ":" + t.pos.y);//old
				t.tileY += t.pos.y / tileSize;
				//t.pos.y = t.pos.y % tileSize;
				float newY = t.pos.y % tileSize;
				if (newY < 0) {
					newY = tileSize + newY;
				}
				t.pos.y = newY;
				System.out.println("YN - " + t.tileY + ":" + t.pos.y);//new
			}
			System.out.println("Local world: " + localTileX + "," + localTileY);
		}
		
		
		//set camera position to entity
		RenderingSystem.getCam().position.x = transform.pos.x ;
		RenderingSystem.getCam().position.y = transform.pos.y;
		
	}
	
	
	@Override
	public void addedToEngine(Engine engine) {
		//this.engine = engine;
		entities = engine.getEntitiesFor(Family.all(TransformComponent.class).get());
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
