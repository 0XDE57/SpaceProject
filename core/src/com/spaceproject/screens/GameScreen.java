package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.SystemPriorityConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.Universe;
import com.spaceproject.generation.noise.NoiseBuffer;
import com.spaceproject.generation.noise.NoiseGenListener;
import com.spaceproject.generation.noise.NoiseThread;
import com.spaceproject.generation.noise.NoiseThreadPoolExecutor;
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
import com.spaceproject.ui.MapState;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GameScreen extends MyScreenAdapter implements NoiseGenListener {

	public Engine engine, persistenceEngine;
	public static long gameTimeCurrent, gameTimeStart;


	private static boolean inSpace;
	private static Entity currentPlanet = null;
	private ImmutableArray<Entity> transitioningEntities;

	public static Universe universe;
	public static LinkedBlockingQueue<NoiseBuffer> noiseBufferQueue;
	public static NoiseThreadPoolExecutor noiseThreadPool;


	ShaderProgram shader = null;


	public GameScreen(boolean inSpace) {
		GameScreen.inSpace = inSpace;

		gameTimeStart = System.nanoTime();

		universe = new Universe();
		noiseBufferQueue = new LinkedBlockingQueue<NoiseBuffer>();
		noiseThreadPool = new NoiseThreadPoolExecutor(SpaceProject.celestcfg.maxGenThreads);
		noiseThreadPool.addListener(universe);
		noiseThreadPool.addListener(this);



		//playing with shaders
		boolean useShader = false;
		if (useShader) {
			//shader = new ShaderProgram(Gdx.files.internal("shaders/quadRotation.vsh"), Gdx.files.internal("shaders/quadRotation.fsh"));
			//shader = new ShaderProgram(Gdx.files.internal("shaders/passthrough.vsh"), Gdx.files.internal("shaders/passthrough.fsh"));
			shader = new ShaderProgram(Gdx.files.internal("shaders/invert.vsh"), Gdx.files.internal("shaders/invert.fsh"));
			ShaderProgram.pedantic = false;
			Gdx.app.log(this.getClass().getSimpleName(), "Shader compiled: " + shader.isCompiled() + ": " + shader.getLog());
			if (shader.isCompiled())
				batch.setShader(shader);
		}



		// load test default values
		Entity playerTESTSHIP = EntityFactory.createPlayerShip(0, 0);
		Entity planet = null;

		if (!inSpace && planet == null) {
			planet = EntityFactory.createPlanet(0, new Entity(), 0, false);
			Gdx.app.log(this.getClass().getSimpleName(), "NULL PLANET: Debug world loaded");
			//int a = 1/0;// throw new Exception("");
		}
		
		if (inSpace) {
			initSpace(playerTESTSHIP);
		} else {
			initWorld(playerTESTSHIP, planet);
		}

		
		//cleanup unmanaged resources
		//new ResourceDisposer(engine);//TODO: this is causing org.lwjgl.opengl.OpenGLException: Cannot use offsets when Array Buffer Object is disabled
	}
	
	public static boolean inSpace() {
		return inSpace;
	}
	
	//region system loading
	private void loadBaseSystems() {
		//systems that are common between space and world
		//eg: input and movement
		
		SystemPriorityConfig cfg = SpaceProject.priorityConfig;
		
		if (SpaceProject.isMobile()) {
			MobileInputSystem mobileInputSystem = new MobileInputSystem();
			mobileInputSystem.priority = cfg.mobileInputSystem;
			engine.addSystem(mobileInputSystem);
		} else {
			DesktopInputSystem desktopInputSystem = new DesktopInputSystem();
			desktopInputSystem.priority = cfg.desktopInputSystem;
			inputMultiplexer.addProcessor(desktopInputSystem);
			engine.addSystem(desktopInputSystem);
		}
		
		AISystem aiSystem = new AISystem();
		aiSystem.priority = cfg.aiSystem;
		engine.addSystem(aiSystem);
		
		
		ControlSystem controlSystem = new ControlSystem();
		controlSystem.priority = cfg.controlSystem;
		engine.addSystem(controlSystem);
		
		//movement
		MovementSystem movementSystem = new MovementSystem();
		movementSystem.priority = cfg.movementSystem;
		engine.addSystem(movementSystem);
		
		BoundsSystem boundsSystem = new BoundsSystem();
		boundsSystem.priority = cfg.boundsSystem;
		engine.addSystem(boundsSystem);
		
		CollisionSystem collisionSystem = new CollisionSystem();
		collisionSystem.priority = cfg.collisionSystem;
		engine.addSystem(collisionSystem);
		
		
		//rendering
		CameraSystem cameraSystem = new CameraSystem();
		cameraSystem.priority = cfg.cameraSystem;
		engine.addSystem(cameraSystem);
		
		HUDSystem hudSystem = new HUDSystem();
		hudSystem.priority = cfg.hudSystem;
		inputMultiplexer.addProcessor(0, hudSystem.getStage());
		engine.addSystem(hudSystem);
		
		ExpireSystem expireSystem = new ExpireSystem(1);
		expireSystem.priority = cfg.expireSystem;
		engine.addSystem(expireSystem);
		
		ScreenTransitionSystem screenTransitionSystem = new ScreenTransitionSystem();
		screenTransitionSystem.priority = cfg.screenTransitionSystem;
		engine.addSystem(screenTransitionSystem);
		
		
		DebugUISystem debugUISystem = new DebugUISystem();
		debugUISystem.priority = cfg.debugUISystem;
		inputMultiplexer.addProcessor(0, debugUISystem.getStage());
		engine.addSystem(debugUISystem);
	}
	
	private void loadSpaceSystems() {
		//systems that are specific to space
		//eg: space renderer and orbit
		
		SystemPriorityConfig cfg = SpaceProject.priorityConfig;
		
		OrbitSystem orbitSystem = new OrbitSystem();
		orbitSystem.priority = cfg.orbitSystem;
		engine.addSystem(orbitSystem);
		
		SpaceRenderingSystem spaceRenderingSystem = new SpaceRenderingSystem();
		spaceRenderingSystem.priority = cfg.spaceRenderingSystem;
		engine.addSystem(spaceRenderingSystem);
		
		SpaceLoadingSystem spaceLoadingSystem = new SpaceLoadingSystem();
		spaceLoadingSystem.priority = cfg.spaceRenderingSystem;
		engine.addSystem(spaceLoadingSystem);
		SpaceParallaxSystem spaceParallaxSystem = new SpaceParallaxSystem();
		spaceParallaxSystem.priority = cfg.spaceParallaxSystem;
		engine.addSystem(spaceParallaxSystem);
	}
	
	private void loadWorldSystems(Entity planet) {
		//systems that are specific to world
		//eg world renderer and wrapping
		
		Gdx.app.log(this.getClass().getSimpleName(), "==========WORLD==========");
		inSpace = false;
		currentPlanet = planet;
		
		int mapSize = planet.getComponent(PlanetComponent.class).mapSize;
		engine.addSystem(new WorldWrapSystem(mapSize));
		
		engine.addSystem(new WorldRenderingSystem(planet));
	}
	

	private void initSpace(Entity transitioningEntity) {
		Gdx.app.log(this.getClass().getSimpleName(), "==========SPACE==========");
		inSpace = true;
		currentPlanet = null;
		
		
		// engine to handle all entities and components
		engine = new Engine();



		//===============SYSTEMS===============
		//input
		if (SpaceProject.isMobile()) {
			engine.addSystem(new MobileInputSystem());
		} else {
			DesktopInputSystem desktopInputSystem = new DesktopInputSystem();
			inputMultiplexer.addProcessor(desktopInputSystem);
			engine.addSystem(desktopInputSystem);
		}
		engine.addSystem(new AISystem());

		//loading
		//TODO: SpaceParallaxSystem & SpaceLoadingSystem are a source of jitter/jumping while moving, caused by loading/unloading textures.
		engine.addSystem(new SpaceLoadingSystem());
		engine.addSystem(new SpaceParallaxSystem());

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
		HUDSystem hudSystem = new HUDSystem();
		inputMultiplexer.addProcessor(0, hudSystem.getStage());
		engine.addSystem(hudSystem);
		DebugUISystem debugUISystem = new DebugUISystem();
		inputMultiplexer.addProcessor(0, debugUISystem.getStage());
		engine.addSystem(debugUISystem);





		//===============ENTITIES===============
		//test ships
		engine.addEntity(EntityFactory.createShip3(-200, 400));
		engine.addEntity(EntityFactory.createShip3(-300, 400));
		engine.addEntity(EntityFactory.createShip3(-400, 400));
		engine.addEntity(EntityFactory.createShip3(-600, 400));

		
		//add player
		engine.addEntity(transitioningEntity);


		Entity aiTest = EntityFactory.createCharacterAI(0, 400);
		Mappers.AI.get(aiTest).state = AIComponent.testState.dumbwander;
		//aiTest.add(ship.remove(CameraFocusComponent.class));//test cam focus on AI
		engine.addEntity(aiTest);

		Entity aiTest2 = EntityFactory.createCharacterAI(0, 600);
		Mappers.AI.get(aiTest2).state = AIComponent.testState.idle;
		engine.addEntity(aiTest2);

		Entity aiTest3 = EntityFactory.createCharacterAI(0, 800);
		Mappers.AI.get(aiTest3).state = AIComponent.testState.landOnPlanet;
		engine.addEntity(aiTest3);


		/*
		Entity test3DEntity = EntityFactory.createShip3(0, -100);
		Texture shipTop = TextureFactory.generateShip(123, 20);
		Texture shipBottom = TextureFactory.generateShipUnderSide(123, 20);
		Sprite3DComponent sprite3DComp = new Sprite3DComponent();
		sprite3DComp.renderable = new Sprite3D(shipTop, shipBottom);
		test3DEntity.remove(TextureComponent.class);
		test3DEntity.add(sprite3DComp);
		engine.addEntity(test3DEntity);
		*/
	}
	//endregion
	
	
	private void initWorld(Entity transitioningEntity, Entity planet) {
		Gdx.app.log(this.getClass().getSimpleName(), "==========WORLD==========");
		inSpace = false;
		currentPlanet = planet;

		int mapSize = planet.getComponent(PlanetComponent.class).mapSize;

		Misc.printObjectFields(planet.getComponent(SeedComponent.class));
		Misc.printObjectFields(planet.getComponent(PlanetComponent.class));
		//Misc.printEntity(transitionComponent.transitioningEntity);



		// engine to handle all entities and components
		engine = new Engine();


		// ===============SYSTEMS===============
		// input
		if (SpaceProject.isMobile()) {
			engine.addSystem(new MobileInputSystem());
		} else {
			DesktopInputSystem desktopInputSystem = new DesktopInputSystem();
			inputMultiplexer.addProcessor(desktopInputSystem);
			engine.addSystem(desktopInputSystem);
		}
		engine.addSystem(new AISystem());

		// loading

		// logic
		engine.addSystem(new ScreenTransitionSystem());
		engine.addSystem(new ControlSystem());
		engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new MovementSystem());
		engine.addSystem(new WorldWrapSystem(mapSize));
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new CollisionSystem());

		// rendering
		engine.addSystem(new CameraSystem());
		engine.addSystem(new WorldRenderingSystem(planet));
		HUDSystem hudSystem = new HUDSystem();
		inputMultiplexer.addProcessor(0, hudSystem.getStage());
		engine.addSystem(hudSystem);
		DebugUISystem debugUISystem = new DebugUISystem();
		inputMultiplexer.addProcessor(0, debugUISystem.getStage());
		engine.addSystem(debugUISystem);



		// ===============ENTITIES===============
		// add player
		Entity ship = transitioningEntity;
		int position = mapSize * SpaceProject.worldcfg.tileSize / 2;//set  position to middle of planet
		ship.getComponent(TransformComponent.class).pos.set(position, position);
		engine.addEntity(ship);

		

		// test ships near player
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

	@Override
	public void threadFinished(NoiseThread noise) {
		noiseBufferQueue.add(noise.getNoise());
	}

	@Override
	public void render(float delta) {
		super.render(delta);

		// update engine
		if (!isPaused) {
			gameTimeCurrent = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - gameTimeStart);
		}
		engine.update(delta);


		checkTransition();

		if (Gdx.input.isKeyJustPressed(Keys.F1)) {
			//debug
			Misc.printEntities(engine);
		}
		
		if (Gdx.input.isKeyJustPressed(Keys.GRAVE)) {//tilda
			if (isPaused) {
				resume();
			} else {
				pause();
			}
		}
		
	}


	private void checkTransition() {
		//TODO: still a bit too much transitions logic in here, should move more to transition system
		//maybe a separete component DoTransitionComp added when stage hits transition, in order to move
		//animation next stage call to system with other similar calls and logic?
		boolean transition = false;
		Entity transEntity = null;
		if (transitioningEntities == null) {
			transitioningEntities = engine.getEntitiesFor(Family.all(ScreenTransitionComponent.class).get());
		}

		for (Entity e : transitioningEntities) {
			ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(e);
			if (screenTrans.doTransition) {
				screenTrans.doTransition = false;

				if (e.getComponent(ControlFocusComponent.class) != null) {
					transition = true;
					transEntity = e;
				}

				if (Mappers.AI.get(e) != null) {
					Gdx.app.log(this.getClass().getSimpleName(), "REMOVING: " + Misc.objString(e));
					engine.removeEntity(e);
					/*//TODO: background stuff
					if (Mappers.persist.get(e)) {
						System.out.println("MOVED to background engine: " + Misc.objString(e));
						persistenceEngine.addEntity(e);
					}*/
				}

				if (inSpace) {
					screenTrans.landStage = screenTrans.landStage.next();
				} else {
					screenTrans.takeOffStage = screenTrans.takeOffStage.next();
				}
			}
		}


		if (transition) {
			//engine.removeEntityListener();
			engine.removeAllEntities();//to fix family references when entities added to new engine
			transitioningEntities = null;
			

			inputMultiplexer.clear();
			if (inSpace) {
				initWorld(transEntity, Mappers.screenTrans.get(transEntity).planet);
			} else {
				Mappers.screenTrans.get(transEntity).planet = currentPlanet;
				initSpace(transEntity);

			}
			inputMultiplexer.addProcessor(0,this);

			/*
			//TODO: background stuff
			for (Entity relevantEntity : backgroundEngine.getEntities()) {
				//if landing on planet, and relevantEntity is on planet, add to engine, remove from backgroundEngine
				//if going to space, and relevantEntity in space, add to engine, remove from backgroundEngine
			}*/

			//Misc.printEntities(engine);
			//Misc.printSystems(engine);
		}
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		
		//todo: move responsibility of this out?
		//approach a: for each system in engine, if implements resizable, fire resize
		//approach b: subscribe to resize event
		HUDSystem hud = engine.getSystem(HUDSystem.class);
		if (hud != null) {
			hud.resize(width, height);
		}
		
		DebugUISystem debugUISystem = engine.getSystem(DebugUISystem.class);
		if (debugUISystem != null) {
			debugUISystem.resize(width, height);
		}
	}

	@Override
	public boolean scrolled(int amount) {
		//TODO: move into hud, hud as input processor
		HUDSystem hud = engine.getSystem(HUDSystem.class);
		if (hud != null) {
			if (hud.getMiniMap().mapState == MapState.full) {
				hud.getMiniMap().scrollMiniMap(amount);
				return false;
			}
		}

		return super.scrolled(amount);
	}

	@Override
	public void dispose() {
		Gdx.app.log(this.getClass().getSimpleName(), "Disposing: " + this.getClass().getSimpleName());

		// clean up after self
		for (EntitySystem sys : engine.getSystems()) {
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}

		
		for (Entity ents : engine.getEntities()) {
			TextureComponent tex = ents.getComponent(TextureComponent.class);
			if (tex != null) {
				tex.texture.dispose();
			}
			
			Sprite3DComponent s3d = Mappers.sprite3D.get(ents);
			if (s3d != null) {
				s3d.renderable.dispose();
			}
		}

		engine.removeAllEntities();
		engine = null;
		
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
		setSystemProcessing(true);
	}
	
	@Override
	public void resume() {
		setSystemProcessing(false);
	}
	
	private void setSystemProcessing(boolean pause) {
		this.isPaused = pause;
		engine.getSystem(ControlSystem.class).setProcessing(isPaused);
		engine.getSystem(MovementSystem.class).setProcessing(isPaused);
		engine.getSystem(CollisionSystem.class).setProcessing(isPaused);
		engine.getSystem(AISystem.class).setProcessing(isPaused);
		engine.getSystem(ExpireSystem.class).setProcessing(isPaused);
		
		OrbitSystem oSys = engine.getSystem(OrbitSystem.class);
		if (oSys != null) oSys.setProcessing(isPaused);
	}

	

}

