package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.systems.BoundsSystem;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.CollisionSystem;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.systems.ExpireSystem;
import com.spaceproject.systems.HUDSystem;
import com.spaceproject.systems.MovementSystem;
import com.spaceproject.systems.OrbitSystem;
import com.spaceproject.systems.PlayerControlSystem;
import com.spaceproject.systems.SpaceLoadingSystem;
import com.spaceproject.systems.SpaceRenderingSystem;
import com.spaceproject.systems.TouchUISystem;

public class SpaceScreen extends ScreenAdapter {

	SpaceProject game;
	
	public static Engine engine;
	
	private static OrthographicCamera cam;
	
	public static CelestialConfig celestcfg;
	public static KeyConfig keycfg;
	
	Entity[] testPlanetsDebug; //test removable planetary system
	
	public SpaceScreen(SpaceProject game) {

		this.game = game;
		
		cam = new OrthographicCamera();
		
		// engine to handle all entities and components
		engine = new Engine();
		
		
		//load values for things like key mapping, settings, default values for generation
		loadConfigs();
		
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
		
		/*
		//add test planetary system (solar system)
		for (Entity entity : EntityFactory.createPlanetarySystem(5000, 5000)) {
			engine.addEntity(entity);
		}
		
		for (Entity entity : EntityFactory.createPlanetarySystem(-5000, -15000)) {
			//engine.addEntity(entity);
		}
		*/

		
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
		
		//engine.addEntity(EntityFactory.createNoiseTile(0, 0, 256));

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
		
		engine.addSystem(new PlayerControlSystem(this, player, playerTESTSHIP));//start as ship
		engine.addSystem(new SpaceRenderingSystem(cam));
		engine.addSystem(new SpaceLoadingSystem(cam));
		engine.addSystem(new MovementSystem());
		engine.addSystem(new OrbitSystem());
		engine.addSystem(new DebugUISystem(cam));
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new CameraSystem(cam));
		engine.addSystem(new CollisionSystem());
		engine.addSystem(new HUDSystem(cam));
		
		//add input system. touch on android and keys on desktop.
		if (Gdx.app.getType() == ApplicationType.Android) {
			engine.addSystem(new TouchUISystem());
		} else {
			engine.addSystem(new DesktopInputSystem());
		}
		
	}

	private static void loadConfigs() {
		//KEYS
		keycfg = new KeyConfig();
		keycfg.loadDefault();
		keycfg.saveToJson();
		
		/*
		FileHandle keyFile = Gdx.files.local("controls.txt");
		if (keyFile.exists()) {
			Json json = new Json();
			json.setUsePrototypes(false);
			
			keycfg = json.fromJson(KeyConfig.class, keyFile.readString());
			System.out.println("Loaded keys from json: " + json.toJson(keycfg));
		} else {
			keycfg = new KeyConfig();
			keycfg.loadDefault();
			//keycfg.saveToJson();
			System.out.println("No key file found. Loaded defaults.");
		}*/
		
		
		//CELESTIAL OBJECTS
		celestcfg = new CelestialConfig();
		celestcfg.loadDefault();
		celestcfg.saveToJson();
	}		
	
	public void render(float delta) {		
		//update engine
		engine.update(delta);
			
		
		//terminate
		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) Gdx.app.exit();			
		
		// [DEBUG]//////////////////////////////
		if (Gdx.input.isKeyJustPressed(keycfg.createSystemAtPlayer)) {
			if (testPlanetsDebug != null) {
				// remove-----------------
				for (Entity entity : testPlanetsDebug) {
					engine.removeEntity(entity);
				}
				testPlanetsDebug = null;
				System.out.println("removed test planets");
			} else {
				// add------------------
				Vector3 pos = cam.position;
				testPlanetsDebug = EntityFactory.createPlanetarySystem(pos.x, pos.y);
				for (Entity entity : testPlanetsDebug) {
					engine.addEntity(entity);
				}
				System.out.println("added test planets");
				//engine.addEntity(EntityFactory.createPlanet(EntityFactory.createStar(0, 0, true), pos.x, pos.y, 0, 0, true));
			}
		}
		// [DEGUB]/////////////////////////////

	}
	
	public void changeScreen(long seed) {
		System.out.println("Change screen to World.");
		
		//dispose();
		
		game.setScreen(new WorldScreen(game, seed));
	}

	public void dispose() {
		//clean up after self
		//dispose of spritebatches and textures
		
		for (EntitySystem sys : engine.getSystems()) {
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}
		
		for (Entity ents : engine.getEntities()) {
			TextureComponent tex = ents.getComponent(TextureComponent.class);
			if (tex != null)
				tex.texture.dispose();
		}
		
		//engine.removeAllEntities();
	}
	
	//resize game
	public void resize(int width, int height) {
		Gdx.app.log("screen", width + ", " + height);
		engine.getSystem(SpaceRenderingSystem.class).resize(width, height);
	}
	
	public void hide() { }

	public void pause() { }

	public void resume() { }

}
