package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.ResourceDisposer;


public class RemovalSystem extends IteratingSystem {
    
    
    public RemovalSystem() {
        super(Family.one(RemoveComponent.class).get());
    }
    
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ResourceDisposer.dispose(entity);
        getEngine().removeEntity(entity);
        
        //respawn player
        ControlFocusComponent controlFocusComp = Mappers.controlFocus.get(entity);
        if (controlFocusComp != null) {
            Gdx.app.log(this.getClass().getSimpleName(), "Controlled entity assumed to be player; respawning");
            Entity newPlayer = EntityFactory.createPlayerShip(0, 0, true);
            getEngine().addEntity(newPlayer);
        }
    }
}
