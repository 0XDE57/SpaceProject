package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.SysCFG;
import com.spaceproject.config.SystemsConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.generation.Universe;
import com.spaceproject.generation.noise.NoiseManager;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.HUDSystem;
import com.spaceproject.ui.MapState;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.ResourceDisposer;
import com.spaceproject.utility.SystemLoader;

import java.util.concurrent.TimeUnit;

public class GameScreen extends MyScreenAdapter {

	public Engine engine, persistenceEngine;
	
	private static long gameTimeCurrent, gameTimeStart, timePaused;
	boolean isPaused = false;
	
	private ImmutableArray<Entity> transitioningEntities;
	
	private static boolean inSpace;
	private Entity currentPlanet = null;
	
	public static Universe universe;
	public static NoiseManager noiseManager;
	

	ShaderProgram shader = null;
	public static String smallFont = "smallFont";
	
	
	public GameScreen(boolean inSpace) {
		GameScreen.inSpace = inSpace;
		
		//init scene2d/VisUI
		if (VisUI.isLoaded())
			VisUI.dispose(true);
		VisUI.load(SpaceProject.isMobile() ? VisUI.SkinScale.X2 : VisUI.SkinScale.X1);
		BitmapFont font = FontFactory.createFont(FontFactory.fontBitstreamVM, 12);
		VisUI.getSkin().add(smallFont, font);
		
		gameTimeStart = System.nanoTime();

		universe = new Universe();
		noiseManager = new NoiseManager(SpaceProject.celestcfg.maxGenThreads);
		

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
		
		engine = new Engine();

		if (inSpace) {
			initSpace(playerTESTSHIP);
		} else {
			initWorld(playerTESTSHIP, planet);
		}
		
	}
	
	
	public static boolean inSpace() {
		return inSpace;
	}

	
	//region system loading
	private void initSpace(Entity transitioningEntity) {
		inSpace = true;
		currentPlanet = null;
		
		SystemLoader.loadSystems(this, engine, inSpace, SpaceProject.systemsConfig);


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
		inSpace = false;
		currentPlanet = planet;
		

		Misc.printObjectFields(planet.getComponent(SeedComponent.class));
		Misc.printObjectFields(planet.getComponent(PlanetComponent.class));
		//Misc.printEntity(transitionComponent.transitioningEntity);
		
		SystemLoader.loadSystems(this, engine, inSpace, SpaceProject.systemsConfig);

		


		// ===============ENTITIES===============
		// add player
		Entity ship = transitioningEntity;
		int mapSize = planet.getComponent(PlanetComponent.class).mapSize;
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
	//endregion
	

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
			setSystemProcessing(!isPaused);
			/*
			if (isPaused) {
				resume();
			} else {
				pause();
			}*/
		}
		
	}


	private void checkTransition() {
		//TODO: still a bit too much transitions logic in here, should move more to transition system
		//maybe a separate component DoTransitionComp added when stage hits transition, in order to move
		//animation next stage call to system with other similar calls and logic?
		boolean transition = false;
		Entity transEntity = null;
		if (transitioningEntities == null) {
			transitioningEntities = engine.getEntitiesFor(Family.all(ScreenTransitionComponent.class).get());
		}

		for (Entity e : transitioningEntities) {
			//todo: what happens (in terms of persistence) to an entity in process of transitioning?
			//if important -> persist
			//if same world (even if not important) -> persist (land on planet at same time as AI=where is AI)
			ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(e);
			if (screenTrans.doTransition) {
				screenTrans.doTransition = false;

				if (e.getComponent(ControlFocusComponent.class) != null) {
					transition = true;
					transEntity = e;
				}

				if (Mappers.AI.get(e) != null) {
					Gdx.app.log(this.getClass().getSimpleName(), "REMOVING: " + Misc.objString(e));
					ResourceDisposer.dispose(e);
					engine.removeEntity(e);
					/*//TODO: persist
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
			ResourceDisposer.disposeAllExcept(engine.getEntities(), transitioningEntities);
			engine.removeAllEntities();//to fix family references when entities added to new engine
			transitioningEntities = null;
			
			
			if (inSpace) {
				initWorld(transEntity, Mappers.screenTrans.get(transEntity).planet);
			} else {
				Mappers.screenTrans.get(transEntity).planet = currentPlanet;
				initSpace(transEntity);
			}

			/*
			//TODO: persist
			for (Entity relevantEntity : backgroundEngine.getEntities()) {
				//if landing on planet, and relevantEntity is on planet, add to engine, remove from backgroundEngine
				//if going to space, and relevantEntity in space, add to engine, remove from backgroundEngine
			}*/
		}
	}
	
	public static long getGameTimeCurrent() {
		return gameTimeCurrent;
	}
	
	public Entity getCurrentPlanet() {
		return currentPlanet;
	}
	
	
	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		
		//todo: fire resize event for systems to subscribe to
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
				//todo: minimap should zoom only on mouse over / focus
				hud.getMiniMap().scrollMiniMap(amount);
				return false;
			}
		}

		return super.scrolled(amount);
	}

	
	//region pause/resume
	@Override
	public void pause() {
		//this is called from on lose focus
		//should be separate & optional
		//if pauseOnLoseFocus, pause.
		//setSystemProcessing(true);
	}
	
	@Override
	public void resume() {
		//called on regain focus
		//if pauseOnLoseFocus, resume
		//setSystemProcessing(false);
	}
	
	private void setSystemProcessing(boolean pause) {
		this.isPaused = pause;
		if (isPaused) {
			timePaused = System.nanoTime();
		} else {
			long delta = System.nanoTime() - timePaused;
			gameTimeStart += delta;
		}
		Gdx.app.log(this.getClass().getSimpleName(),"paused [" + pause + "]");

		
		SystemsConfig systemsConfig = SpaceProject.systemsConfig;
		for (EntitySystem system : engine.getSystems()) {
			SysCFG sysCFG = systemsConfig.getConfig(system.getClass().getName());
			if (sysCFG.isHaltOnGamePause()) {
				system.setProcessing(!isPaused);
			}
		}
	}
	//endregion
	
	@Override
	public void hide() {
		dispose();
	}
	
	@Override
	public void dispose() {
		Gdx.app.log(this.getClass().getSimpleName(), "Disposing: " + this.getClass().getSimpleName());
		
		// clean up after self
		for (EntitySystem sys : engine.getSystems()) {
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}
		
		ResourceDisposer.disposeAll(engine.getEntities());
		
		engine.removeAllEntities();
		engine = null;
		
	}
	
}

