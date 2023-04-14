package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class RespawnSystem extends EntitySystem implements EntityListener {
    
    //rough draft, don't use

    @Override
    public void entityAdded(Entity entity) { }
    
    @Override
    public void entityRemoved(Entity entity) {
        if (Mappers.controlFocus.get(entity) == null) {
            return;
        }
        
        Gdx.app.log(getClass().getSimpleName(), "Controlled entity assumed to be player; respawning...");
        
        if (GameScreen.inSpace()) {
            Array<Entity> newPlayer = EntityBuilder.createPlayerShip(0, 0, true);
            for (Entity e : newPlayer) {
                getEngine().addEntity(e);
            }
        } else {
            WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
            int mapSize = GameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
            int position = mapSize * worldCFG.tileSize / 2;//set  position to middle of planet
            Entity newPlayer = EntityBuilder.createPlayer(position, position);
            getEngine().addEntity(newPlayer);
        }
    }
  
}
