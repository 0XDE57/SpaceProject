package com.spaceproject.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IntervalSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.spaceproject.components.ExpireComponent;

public class ExpireSystem extends IntervalSystem {
	
	private ComponentMapper<ExpireComponent> expireMap;
	private ImmutableArray<Entity> entities;
	private Engine engine;
	
	public ExpireSystem(float interval) {
		super(interval);

	}

	@SuppressWarnings("unchecked")
	@Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(ExpireComponent.class).get());
        expireMap = ComponentMapper.getFor(ExpireComponent.class);
        this.engine = engine;
    }
	

	@Override
	protected void updateInterval() {
		
		//remove entities when their time runs out
		for (Entity entity : entities) {
			ExpireComponent expire = expireMap.get(entity);
			
			expire.time -= 1;
			
			if (expire.time <= 0) {
				engine.removeEntity(entity);
			}
		}
	
	}


}
