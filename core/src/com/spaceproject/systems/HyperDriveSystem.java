package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class HyperDriveSystem extends IteratingSystem {
    
    public HyperDriveSystem() {
        super(Family.all(TransformComponent.class, HyperDriveComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
        if (hyperDrive.active) {
            TransformComponent transform = Mappers.transform.get(entity);
            transform.pos.add(hyperDrive.velocity.cpy().scl(deltaTime));
        }
    }
    
}
