package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.math.MyMath;

public class HyperDriveSystem extends IteratingSystem {
    
    public HyperDriveSystem() {
        super(Family.all(TransformComponent.class, HyperDriveComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
        
        toggleHyperDrive(entity, hyperDrive);
        
        if (hyperDrive.isActive) {
            TransformComponent transform = Mappers.transform.get(entity);
            transform.pos.add(hyperDrive.velocity.cpy().scl(deltaTime));
        }
    }
    
    private void toggleHyperDrive(Entity entity, HyperDriveComponent hyperDrive) {
        if (hyperDrive.activate && hyperDrive.coolDownTimer.tryEvent()) {
            PhysicsComponent physicsComp = Mappers.physics.get(entity);
            if (hyperDrive.isActive) {
                hyperDrive.isActive = false;
                physicsComp.body.setTransform(entity.getComponent(TransformComponent.class).pos, physicsComp.body.getAngle());
                physicsComp.body.setActive(true);
                physicsComp.body.setLinearVelocity(MyMath.vector(physicsComp.body.getAngle(), 20));
            } else {
                hyperDrive.isActive = true;
                hyperDrive.velocity.set(MyMath.vector(physicsComp.body.getAngle(), hyperDrive.speed));
                physicsComp.body.setActive(false);
            }
            
        }
    }
    
}
