package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IntervalSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.utility.Mappers;

public class ExpireSystem extends IntervalSystem {
    
    private ImmutableArray<Entity> entities;
    
    public ExpireSystem() {
        super(1);
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(ExpireComponent.class).get());
    }
    
    @Override
    protected void updateInterval() {
        //remove entities when their time runs out
        for (Entity entity : entities) {
            ExpireComponent expire = Mappers.expire.get(entity);
            
            expire.time -= 1;
            
            if (expire.time <= 0) {
                entity.add(new RemoveComponent());
            }
        }
    }
    
}
