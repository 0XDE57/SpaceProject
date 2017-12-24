package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.LandConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.systems.AISystem;
import com.spaceproject.systems.BoundsSystem;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.CollisionSystem;
import com.spaceproject.systems.ControlSystem;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.systems.ExpireSystem;
import com.spaceproject.systems.HUDSystem;
import com.spaceproject.systems.MobileInputSystem;
import com.spaceproject.systems.MovementSystem;
import com.spaceproject.systems.OrbitSystem;
import com.spaceproject.systems.ScreenTransitionSystem;
import com.spaceproject.systems.SpaceLoadingSystem;
import com.spaceproject.systems.SpaceParallaxSystem;
import com.spaceproject.systems.SpaceRenderingSystem;
import com.spaceproject.systems.WorldRenderingSystem;
import com.spaceproject.systems.WorldWrapSystem;
import com.spaceproject.utility.MyScreenAdapter;

public class GameScreen extends MyScreenAdapter {

	public Engine engine;
	public static boolean inSpace;
	public static LandConfig landCFG = null;
	public static boolean transition;
	
	public GameScreen(LandConfig landCFG, boolean inSpace) {

		// load test default values
		landCFG = new LandConfig();
		landCFG.position = new Vector3();// start player at 0,0
		Entity player = EntityFactory.createCharacter(landCFG.position.x, landCFG.position.y);
		Entity playerTESTSHIP = EntityFactory.createShip3(landCFG.position.x, landCFG.position.y, landCFG.shipSeed, player);
		landCFG.ship = playerTESTSHIP;

		// test values for a default world
		PlanetComponent planet = new PlanetComponent();
		planet.mapSize = 128;
		planet.scale = 100;
		planet.octaves = 4;
		planet.persistence = 0.68f;
		planet.lacunarity = 2.6f;
		landCFG.planet = planet;
		
		GameScreen.inSpace = inSpace;
		
		if (inSpace) {
			initSpace(landCFG);
		} else {
			initWorld(landCFG);
		}		
	}

	private void initSpace(LandConfig landCFG) {
		System.out.println("==========SPACE==========");
		GameScreen.landCFG = landCFG;
		inSpace = true;
		
		
		// engine to handle all entities and components
		engine = new Engine();
		

		//===============ENTITIES===============

		//test ships
		engine.addEntity(EntityFactory.createShip3(-100, 400));
		engine.addEntity(EntityFactory.createShip3(-200, 400));		
		engine.addEntity(EntityFactory.createShip3(-300, 400));
		engine.addEntity(EntityFactory.createShip3(-400, 400));		

		Entity aiTest = EntityFactory.createCharacterAI(0, 400);
		//aiTest.add(new CameraFocusComponent());
		engine.addEntity(aiTest);
		
		//add player
		Entity ship = landCFG.ship;
		ship.add(new CameraFocusComponent());
		ship.add(new ControlFocusComponent());
		ship.getComponent(TransformComponent.class).pos.x = landCFG.position.x;
		ship.getComponent(TransformComponent.class).pos.y = landCFG.position.y;
		engine.addEntity(ship);
		System.out.println("ship: " + String.format("%X", ship.hashCode()));
		
		
		//===============SYSTEMS===============
		//input
		if (Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.iOS) {
			engine.addSystem(new MobileInputSystem());
		} else {
			engine.addSystem(new DesktopInputSystem());
		}
		engine.addSystem(new AISystem());
		
		//loading
		engine.addSystem(new SpaceLoadingSystem());
		engine.addSystem(new SpaceParallaxSystem());
		//Ai...
		
		//logic
		engine.addSystem(new ScreenTransitionSystem());	
		engine.addSystem(new ControlSystem(this));
		engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new OrbitSystem());
		engine.addSystem(new MovementSystem());
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new CollisionSystem());
		
		
		//rendering
		engine.addSystem(new CameraSystem());
		engine.addSystem(new SpaceRenderingSystem());
		engine.addSystem(new HUDSystem());
		engine.addSystem(new DebugUISystem());
		
		//DebugUISystem.printEntities(engine);
		//DebugUISystem.printSystems(engine);
	}
	
	
	private void initWorld(LandConfig landCFG) {
		System.out.println("==========WORLD==========");
		GameScreen.landCFG = landCFG;
		inSpace = false;

		// engine to handle all entities and components
		engine = new Engine();

		// ===============ENTITIES===============
		// add player
		Entity ship = landCFG.ship;
		int position = landCFG.planet.mapSize * SpaceProject.tileSize / 2;//set  position to middle of planet
		ship.getComponent(TransformComponent.class).pos.x = position;
		ship.getComponent(TransformComponent.class).pos.y = position;
		//ship.add(new ControllableComponent());
		//ship.add(new CameraFocusComponent());
		ship.add(new ControlFocusComponent());
		engine.addEntity(ship);
		System.out.println("ship: " + String.format("%X", ship.hashCode()));

		

		// test ships near player
		engine.addEntity(EntityFactory.createShip3(position + 100, position + 300));
		engine.addEntity(EntityFactory.createShip3(position - 100, position + 300));

		Entity aiTest = EntityFactory.createCharacterAI(position, position + 50);
		//aiTest.add(new CameraFocusComponent());
		engine.addEntity(aiTest);

		
		// ===============SYSTEMS===============
		// input
		if (Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.iOS) {
			engine.addSystem(new MobileInputSystem());
		} else {
			engine.addSystem(new DesktopInputSystem());
		}
		engine.addSystem(new AISystem());

		// loading

		// logic
		engine.addSystem(new ScreenTransitionSystem());
		engine.addSystem(new ControlSystem(this));
		engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new MovementSystem());
		engine.addSystem(new WorldWrapSystem(landCFG.planet.mapSize));
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new CollisionSystem());

		// rendering
		engine.addSystem(new CameraSystem());
		engine.addSystem(new WorldRenderingSystem(landCFG.planet));
		engine.addSystem(new HUDSystem());
		engine.addSystem(new DebugUISystem());
		
		
		//DebugUISystem.printEntities(engine);
		//DebugUISystem.printSystems(engine);
	}

	@Override
	public void render(float delta) {
		super.render(delta);

		// update engine
		engine.update(delta);


		if (Gdx.input.isKeyJustPressed(Keys.U)) {
			DebugUISystem.printEntities(engine);
		}

		//if (Gdx.input.isKeyJustPressed(Keys.H)) { transition = true; }
		if (transition) {
			transition = false;

			//dispose();
			engine.removeAllEntities();
			if (inSpace) {
				initWorld(landCFG);
			} else {
				initSpace(landCFG);
			}

		}
	}

	@Override
	public void dispose() {
		System.out.println("Disposing: " + this.getClass().getSimpleName());

		// clean up after self
		// dispose of spritebatches and textures
		for (EntitySystem sys : engine.getSystems()) {
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}

		//TODO: use entity listener instead
		//Family family = Family.all(TextureComponent.class).get();
		//engine.addEntityListener(family, listener);
		//github.com/libgdx/ashley/wiki/How-to-use-Ashley#entity-events
		for (Entity ents : engine.getEntitiesFor(Family.all(TextureComponent.class).get())) {
			TextureComponent tex = ents.getComponent(TextureComponent.class);
			if (tex != null)
				tex.texture.dispose();
		}

		engine.removeAllEntities();//THIS FIXES IT!
		engine = null;


		//System.gc();//not good practice, just testing

		//super.dispose();
	}

	@Override
	public void hide() {
		// dispose();
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
