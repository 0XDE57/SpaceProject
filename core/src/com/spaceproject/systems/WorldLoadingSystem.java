package com.spaceproject.systems;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;

public class WorldLoadingSystem extends EntitySystem implements IRequireGameContext {
    
    private WorldConfig worldCFG;
    private int mapSize;
    
    @Override
    public void addedToEngine(Engine engine) {
        initMobs(engine);
    }
    
    @Override
    public void initContext(GameScreen gameScreen) {
        mapSize = gameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
        worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
    }
    
    private void initMobs(Engine engine) {
        //a placeholder to add dummy objects for now
        
        // test ships
        int position = mapSize * worldCFG.tileSize / 2;//set  position to middle of planet
        
        engine.addEntity(EntityFactory.createShip3(position + 10, position + 10, GameScreen.getInstance().inSpace()));
        engine.addEntity(EntityFactory.createShip3(position - 10, position + 10, GameScreen.getInstance().inSpace()));
        
        Entity aiTest = EntityFactory.createCharacterAI(position, position + 10);
        Mappers.AI.get(aiTest).state = AIComponent.testState.dumbwander;
        engine.addEntity(aiTest);

		/*
		Entity aiTest2 = EntityFactory.createCharacterAI(position, position - 500);
		Mappers.AI.get(aiTest2).state = AIComponent.testState.takeOffPlanet;
		engine.addEntity(aiTest2);
		*/
        engine.addEntity(EntityFactory.createWall(position + 5, position + 5, 8, 16));
        engine.addEntity(EntityFactory.createWall(position + 9, position + 5, 16, 8));
    }
    
    
}
