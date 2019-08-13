package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

@Deprecated
public class MovementSystem extends IteratingSystem {
    
    //temporary
    private Vector2 tmp = new Vector2();
    
    public MovementSystem() {
        super(Family.all(TransformComponent.class).get());
    }
    
    @Override
    public void processEntity(Entity entity, float deltaTime) {
        TransformComponent position = Mappers.transform.get(entity);
        
        //set velocity
        tmp.set(position.accel).scl(deltaTime);
        position.velocity.add(tmp);
        
        //add velocity to position
        tmp.set(position.velocity).scl(deltaTime);
        position.pos.add(tmp.x, tmp.y);
    }
}
