package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.GameScreen;
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
        
        //attempt to respawn player
        respawnPlayer(entity);
    }
    
    private void respawnPlayer(Entity entity) {
        //todo: perhaps should not be responsibility of removal system. player spawn system?
        ControlFocusComponent controlFocusComp = Mappers.controlFocus.get(entity);
        if (controlFocusComp != null) {
            Gdx.app.log(this.getClass().getSimpleName(), "Controlled entity assumed to be player; respawning");
            if (GameScreen.inSpace()) {
                Entity newPlayer = EntityFactory.createPlayerShip(0, 0, true);
                getEngine().addEntity(newPlayer);
            } else {
                WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
                int mapSize = GameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
                int position = mapSize * worldCFG.tileSize / 2;//set  position to middle of planet
                Entity newPlayer = EntityFactory.createPlayer(position, position);
                getEngine().addEntity(newPlayer);
            }
        }
    }
    
}
