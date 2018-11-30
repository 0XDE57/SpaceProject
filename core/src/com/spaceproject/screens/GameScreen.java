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
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
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

	public Engine engine, backgroundEngine;
	public static long gameTimeCurrent, gameTimeStart;


	public static boolean inSpace;
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
			System.out.println("Shader compiled: " + shader.isCompiled() + ": " + shader.getLog());
			if (shader.isCompiled())
				batch.setShader(shader);
		}



		// load test default values
		Entity playerTESTSHIP = createPlayerShip();
		Entity planet = null;

		if (!inSpace && planet == null) {
			planet = EntityFactory.createPlanet(0, new Entity(), 0, false);
			System.out.println("NULL PLANET: Debug world loaded");
			//int a = 1/0;// throw new Exception("");
		}
		
		if (inSpace) {
			initSpace(playerTESTSHIP);
		} else {
			initWorld(playerTESTSHIP, planet);
		}

	}

	private Entity createPlayerShip() {
		Entity player = EntityFactory.createCharacter(0, 0);
		Entity playerTESTSHIP = EntityFactory.createShip3(0, 0, 0, player);
		playerTESTSHIP.add(new CameraFocusComponent());
		playerTESTSHIP.add(new ControlFocusComponent());
		return playerTESTSHIP;
	}

	private void initSpace(Entity transitioningEntity) {
		System.out.println("==========SPACE==========");
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
		engine.addSystem(new DebugUISystem());






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
	
	
	private void initWorld(Entity transitioningEntity, Entity planet) {
		System.out.println("==========WORLD==========");
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
		engine.addSystem(new DebugUISystem());



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
		gameTimeCurrent = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - gameTimeStart);
		engine.update(delta);


		checkTransition();

		if (Gdx.input.isKeyJustPressed(Keys.F1)) {
			//debug
			Misc.printEntities(engine);
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
					System.out.println("REMOVING: " + Misc.myToString(e));
					engine.removeEntity(e);
						/*//TODO: background stuff
						if (Mappers.persist.get(e)) {
							System.out.println("MOVED to background engine: " + Misc.myToString(e));
							backgroundEngine.addEntity(e);
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
			engine.removeAllEntities();//to fix family references when entities added to new engine
			transitioningEntities = null;
			//engine.removeAllSystems();?

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
		HUDSystem hud = engine.getSystem(HUDSystem.class);
		if (hud != null) {
			hud.resize(width, height);
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
		System.out.println("Disposing: " + this.getClass().getSimpleName());

		// clean up after self
		// dispose of spritebatches and textures
		for (EntitySystem sys : engine.getSystems()) {
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}

		for (Entity ents : engine.getEntitiesFor(Family.all(TextureComponent.class).get())) {
			TextureComponent tex = ents.getComponent(TextureComponent.class);
			if (tex != null)
				tex.texture.dispose();
		}

		engine.removeAllEntities();
		engine = null;

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
