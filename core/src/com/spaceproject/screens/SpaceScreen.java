package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.systems.BoundsSystem;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.CollisionSystem;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.systems.ExpireSystem;
import com.spaceproject.systems.HUDSystem;
import com.spaceproject.systems.LoadingSystem;
import com.spaceproject.systems.MovementSystem;
import com.spaceproject.systems.OrbitSystem;
import com.spaceproject.systems.PlayerControlSystem;
import com.spaceproject.systems.RenderingSystem;
import com.spaceproject.systems.TouchUISystem;

public class SpaceScreen extends ScreenAdapter {

	SpaceProject game;
	
	public Engine engine;
	
		
	public SpaceScreen(SpaceProject game) {

		this.game = game;
		// engine to handle all entities and component
		engine = new Engine();
		
		
		//temporary test entities--------------------------------------------
		//add entities to engine, should put in spawn system or initializer of sorts...
		//TODO: need refactor and a home			
		
		/*
		//test planets
		engine.addEntity(EntityFactory.createPlanet(300, 300));
		engine.addEntity(EntityFactory.createPlanet(-300, -300));
		engine.addEntity(EntityFactory.createPlanet(600, 0));
		engine.addEntity(EntityFactory.createPlanet(-600, 0));
		engine.addEntity(EntityFactory.createPlanet(1900, 0));
		*/
		
		//add test planetary system (solar system)
		for (Entity entity : EntityFactory.createPlanetarySystem(5000, 5000)) {
			engine.addEntity(entity);
		}
		
		for (Entity entity : EntityFactory.createPlanetarySystem(-5000, -15000)) {
			//engine.addEntity(entity);
		}


		
		//test ships
		//engine.addEntity(EntityFactory.createShip(100, 300));		
		//engine.addEntity(EntityFactory.createShip(0, 300));
		
		engine.addEntity(EntityFactory.createShip3(-100, 400));
		engine.addEntity(EntityFactory.createShip3(-200, 400));		
		engine.addEntity(EntityFactory.createShip3(-300, 400));
		engine.addEntity(EntityFactory.createShip3(-400, 400));
		
		//engine.addEntity(EntityFactory.createShip3(200, 400));
		//engine.addEntity(EntityFactory.createShip3(300, 400));
		//engine.addEntity(EntityFactory.createShip3(400, 400));
		
		//player------------------------------------------
		//start as player
		//Entity player = EntityFactory.createCharacter(0, 0, false, null);
		//engine.addEntity(player);
		
		//start as ship
		Entity playerTESTSHIP = EntityFactory.createShip3(0, 0);
		Entity player = EntityFactory.createCharacter(0, 0);
		
		
		playerTESTSHIP.add(new PlayerFocusComponent());
		engine.addEntity(playerTESTSHIP);

		/*
		Entity testOrbit = EntityFactory.createPlanet(playerTESTSHIP, 755, true);
		testOrbit.getComponent(OrbitComponent.class).orbitSpeed = 0.3f;
		testOrbit.getComponent(OrbitComponent.class).angle = 3.14f*2;
		engine.addEntity(testOrbit);
		
		Entity testOrbit2 = EntityFactory.createPlanet(playerTESTSHIP, 1000, false);
		testOrbit2.getComponent(OrbitComponent.class).orbitSpeed = 0.3f;
		testOrbit2.getComponent(OrbitComponent.class).angle = 3.14f*2;
		engine.addEntity(testOrbit2);
		
		Entity testMap = new Entity();
		testMap.add(new MapComponent());
		TransformComponent t = new TransformComponent();
		t.pos.x = 1100;
		t.pos.y = 500;
		testMap.add(t);
		engine.addEntity(testMap);
		*/
		
		// Add systems to engine---------------------------------------------------------
		//engine.addSystem(new PlayerControlSystem(player));//start as player
		
		engine.addSystem(new PlayerControlSystem(player, playerTESTSHIP));//start as ship
		engine.addSystem(new RenderingSystem());
		engine.addSystem(new LoadingSystem());
		engine.addSystem(new MovementSystem());
		engine.addSystem(new OrbitSystem());
		engine.addSystem(new DebugUISystem());
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new CameraSystem());
		engine.addSystem(new CollisionSystem());
		engine.addSystem(new HUDSystem());
		
		//add input system. touch on android and keys on desktop.
		if (Gdx.app.getType() == ApplicationType.Android) {
			engine.addSystem(new TouchUISystem());
		} else {
			engine.addSystem(new DesktopInputSystem());
		}
		
	}	
	Entity[] testPlanetsDebug; //test removable planetary system
	
	public void render(float delta) {		
		//update engine
		engine.update(delta);
			
		
		//terminate
		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) Gdx.app.exit();			
		
		// [DEBUG]//////////////////////////////
		if (Gdx.input.isKeyJustPressed(Keys.K)) {
			if (testPlanetsDebug != null) {
				// remove-----------------
				for (Entity entity : testPlanetsDebug) {
					engine.removeEntity(entity);
				}
				testPlanetsDebug = null;
				System.out.println("removed test planets");
			} else {
				// add------------------
				Vector3 pos = engine.getSystem(RenderingSystem.class).getCam().position;
				testPlanetsDebug = EntityFactory.createPlanetarySystem(pos.x, pos.y);
				for (Entity entity : testPlanetsDebug) {
					engine.addEntity(entity);
				}
				System.out.println("added test planets");
			}
		}
		// [DEGUB]/////////////////////////////

	}

	//resize game
	public void resize(int width, int height) {
		Gdx.app.log("screen", width + ", " + height);
		engine.getSystem(RenderingSystem.class).resize(width, height);
	}

	
	public void dispose() {
		//TODO: clean up after self
		//dispose of all spritebatches and whatnot
		//create dispose method in all systems and call?
		
	}
	
	public void hide() { }

	public void pause() { }

	public void resume() { }
}
