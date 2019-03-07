package com.spaceproject.systems;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;

public class WorldLoadingSystem extends EntitySystem implements IRequireGameContext {
    
    int mapSize;
    
    @Override
    public void addedToEngine(Engine engine) {
        initMobs(engine);
    }
    
    @Override
    public void initContext(GameScreen gameScreen) {
        mapSize = gameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
    }
    
    private void initMobs(Engine engine) {
        //a placeholder to add dummy objects for now
        
        // test ships
        int position = mapSize * SpaceProject.worldcfg.tileSize / 2;//set  position to middle of planet
        
        engine.addEntity(EntityFactory.createShip3(position + 100, position + 600));
        engine.addEntity(EntityFactory.createShip3(position - 100, position + 600));
        
        Entity aiTest = EntityFactory.createCharacterAI(position, position + 50);
        Mappers.AI.get(aiTest).state = AIComponent.testState.dumbwander;
        engine.addEntity(aiTest);

		/*
		Entity aiTest2 = EntityFactory.createCharacterAI(position, position - 500);
		Mappers.AI.get(aiTest2).state = AIComponent.testState.takeOffPlanet;
		engine.addEntity(aiTest2);
		*/
    }
    
    
}
