package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.config.LandConfig;
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
import com.spaceproject.utility.MyScreenAdapter;

public class SpaceScreen extends MyScreenAdapter {

	SpaceProject game;
	
	public static Engine engine;
	
	public static CelestialConfig celestcfg;
	public static KeyConfig keycfg;
	
	LandConfig landCFG;	
	
	Entity[] testPlanetsDebug; //test removable planetary system
	
	public SpaceScreen(SpaceProject game, LandConfig landCFG) {

		this.game = game;
		
		
		// engine to handle all entities and components
		engine = new Engine();
		
		
		//load values for things like key mapping, settings, default values for generation
		loadConfigs();
		
		
		//add temporary test entities--------------------------------------------
		//TODO: need refactor, put in spawn system or initializer of sorts...			
		//add test planetary system near spawn
		//for (Entity entity : EntityFactory.createPlanetarySystem(700, 700)) { engine.addEntity(entity);}
		
		//add test ships
		engine.addEntity(EntityFactory.createShip3(-100, 400));
		engine.addEntity(EntityFactory.createShip3(-200, 400));		
		engine.addEntity(EntityFactory.createShip3(-300, 400));
		engine.addEntity(EntityFactory.createShip3(-400, 400));
			
		//add player
		boolean startAsShip = true;//debug: start as ship or player
		Entity playerTESTSHIP = EntityFactory.createShip3(landCFG.position.x, landCFG.position.y, landCFG.shipSeed);
		Entity player = EntityFactory.createCharacter(landCFG.position.x, landCFG.position.y);
		
		if (startAsShip) {
			//start as ship	
			playerTESTSHIP.add(new CameraFocusComponent());
			engine.addEntity(playerTESTSHIP);
		} else {
			//start as player
			player.add(new CameraFocusComponent());
			engine.addEntity(player);
		}
		
		// Add systems to engine---------------------------------------------------------
		if (startAsShip) {
			engine.addSystem(new PlayerControlSystem(this, player, playerTESTSHIP, landCFG));//start as ship
		} else {
			engine.addSystem(new PlayerControlSystem(this, player, landCFG));//start as player
		}		
		
		engine.addSystem(new SpaceRenderingSystem());
		engine.addSystem(new SpaceLoadingSystem(cam));
		engine.addSystem(new MovementSystem());
		engine.addSystem(new OrbitSystem());
		engine.addSystem(new DebugUISystem());
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new CameraSystem());
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
		//set vsync off for development, on by default
		toggleVsync();
		
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
	
	@Override
	public void render(float delta) {
		super.render(delta);
		
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
	
	public void changeScreen(LandConfig landCFG) {
		System.out.println("Change screen to World.");
		
		//dispose();
		
		game.setScreen(new WorldScreen(game, landCFG));
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

	public void hide() { }

	public void pause() { }

	public void resume() { }

}
