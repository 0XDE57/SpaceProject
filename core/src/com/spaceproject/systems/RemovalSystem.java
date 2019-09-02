package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.utility.ResourceDisposer;


public class RemovalSystem extends IteratingSystem {
    
    
    public RemovalSystem() {
        super(Family.one(RemoveComponent.class).get());
    }
    
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ResourceDisposer.dispose(entity);
        getEngine().removeEntity(entity);
    }
}
