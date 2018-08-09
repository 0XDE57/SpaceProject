package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.SeedComponent;
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
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyScreenAdapter;

import java.util.concurrent.TimeUnit;

public class GameScreen extends MyScreenAdapter {

	public Engine engine;
	public static boolean inSpace;
	public static long gameTimeCurrent, gameTimeStart;

	ShaderProgram shader = new ShaderProgram(Gdx.files.internal("shaders/quadRotation.vsh"), Gdx.files.internal("shaders/quadRotation.fsh"));


	public static LandConfig landCFG = null;//todo: remove this when switch to state entity
	public static boolean transition;//todo: remove this when switch to state entity


	public GameScreen(boolean inSpace) {
		GameScreen.inSpace = inSpace;

		gameTimeStart = System.nanoTime();

		/*
		//playing with shaders
		ShaderProgram.pedantic = false;
		System.out.println("Shader compiled: " + shader.isCompiled() + ": " + shader.getLog());
		batch.setShader(shader);
		*/

		// load test default values
		landCFG = new LandConfig();
		landCFG.position = new Vector3();// start player at 0,0
		Entity player = EntityFactory.createCharacter(landCFG.position.x, landCFG.position.y);
		Entity playerTESTSHIP = EntityFactory.createShip3(landCFG.position.x, landCFG.position.y, 0, player);
		landCFG.ship = playerTESTSHIP;



		// test values for a default world
		if (!inSpace && landCFG.planet == null) {
			SeedComponent seed = new SeedComponent();
			seed.seed = 0;

			PlanetComponent planet = new PlanetComponent();
			planet.mapSize = 128;
			planet.scale = 100;
			planet.octaves = 4;
			planet.persistence = 0.68f;
			planet.lacunarity = 2.6f;
			landCFG.planet = planet;
			landCFG.seed = seed;

			System.out.println("NULL PLANET: Default world loaded");
		}

		
		if (inSpace) {
			initSpace(landCFG);
		} else {
			initWorld(landCFG);
		}		
	}

	private void initSpace(LandConfig landCFG) {
		System.out.println("==========SPACE==========");
		GameScreen.landCFG = landCFG;
		/*
		Misc.printObjectFields(landCFG);
		Misc.printEntity(landCFG.ship);
		*/

		inSpace = true;
		
		
		// engine to handle all entities and components
		engine = new Engine();



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
		engine.addSystem(new SpaceParallaxSystem());//TODO: this is the source of jiggering while moving, caused by loading/unloading tiles.
		//Ai...

		//logic
		engine.addSystem(new ScreenTransitionSystem());
		engine.addSystem(new ControlSystem());
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






		//===============ENTITIES===============
		//test ships
		engine.addEntity(EntityFactory.createShip3(-100, 400));
		//engine.addEntity(EntityFactory.createShip3(-200, 400));
		//engine.addEntity(EntityFactory.createShip3(-300, 400));
		//engine.addEntity(EntityFactory.createShip3(-400, 400));



		
		//add player
		Entity ship = landCFG.ship;
		ship.add(new CameraFocusComponent());
		ship.add(new ControlFocusComponent());
		//ship.add(new ControllableComponent());
		ship.getComponent(TransformComponent.class).pos.x = landCFG.position.x;
		ship.getComponent(TransformComponent.class).pos.y = landCFG.position.y;
		engine.addEntity(ship);
		//System.out.println("shipTex: " + String.format("%X", shipTex.hashCode()));

		Entity aiTest = EntityFactory.createCharacterAI(0, 400);
		//engine.addEntity(aiTest);
		//aiTest.add(ship.remove(CameraFocusComponent.class));
		







		//DebugUISystem.printEntities(engine);
		//DebugUISystem.printSystems(engine);
	}
	
	
	private void initWorld(LandConfig landCFG) {
		System.out.println("==========WORLD==========");
		//Misc.printObjectFields(landCFG);
		Misc.printObjectFields(landCFG.seed);
		Misc.printObjectFields(landCFG.planet);
		//Misc.printEntity(landCFG.ship);

		GameScreen.landCFG = landCFG;
		inSpace = false;

		// engine to handle all entities and components
		engine = new Engine();


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
		engine.addSystem(new ControlSystem());
		engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new MovementSystem());
		engine.addSystem(new WorldWrapSystem(landCFG.planet.mapSize));
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new CollisionSystem());

		// rendering
		engine.addSystem(new CameraSystem());
		engine.addSystem(new WorldRenderingSystem(landCFG.seed, landCFG.planet));
		engine.addSystem(new HUDSystem());
		engine.addSystem(new DebugUISystem());



		// ===============ENTITIES===============
		// add player
		Entity ship = landCFG.ship;

		int position = landCFG.planet.mapSize * SpaceProject.tileSize / 2;//set  position to middle of planet
		ship.getComponent(TransformComponent.class).pos.x = position;
		ship.getComponent(TransformComponent.class).pos.y = position;
		//shipTex.add(new ControllableComponent());
		//shipTex.add(new CameraFocusComponent());
		//ship.add(new ControllableComponent());
		ship.add(new ControlFocusComponent());
		engine.addEntity(ship);
		//System.out.println("shipTex: " + String.format("%X", shipTex.hashCode()));

		

		// test ships near player
		engine.addEntity(EntityFactory.createShip3(position + 100, position + 300));
		//engine.addEntity(EntityFactory.createShip3(position - 100, position + 300));

		Entity aiTest = EntityFactory.createCharacterAI(position, position + 50);
		//aiTest.add(new CameraFocusComponent());
		engine.addEntity(aiTest);

		

		
		//DebugUISystem.printEntities(engine);
		//DebugUISystem.printSystems(engine);
	}


	@Override
	public void render(float delta) {
		super.render(delta);

		// update engine
		gameTimeCurrent = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - gameTimeStart);
		engine.update(delta);

		engine.getSystem(DebugUISystem.class).drawMessage("time: " + gameTimeCurrent, 500, Gdx.graphics.getHeight()-10);

		if (Gdx.input.isKeyJustPressed(Keys.U)) {
			Misc.printEntities(engine);
		}


		if (transition) {
			transition = false;

			//dispose();
			engine.removeAllEntities();
			//engine.removeAllSystems();?

			if (inSpace) {
				initWorld(landCFG);
			} else {
				initSpace(landCFG);
			}

		}

		if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
			if (HUDSystem.drawMap != HUDSystem.MapState.off) {
				HUDSystem.spaceMapScale = 500;
			}
		}

		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			game.setScreen(new MainMenuScreen(game));
		}
	}

	@Override
	public boolean scrolled(int amount) {
		if (HUDSystem.drawMap == HUDSystem.MapState.full) {
			HUDSystem.spaceMapScale += amount*20;
			return false;
		} else {
			return super.scrolled(amount);
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
