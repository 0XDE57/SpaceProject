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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.SysCFG;
import com.spaceproject.config.SystemsConfig;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.generation.Universe;
import com.spaceproject.generation.noise.NoiseManager;
import com.spaceproject.systems.HUDSystem;
import com.spaceproject.systems.ScreenTransitionSystem;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.IScreenResizeListener;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.ResourceDisposer;
import com.spaceproject.utility.SystemLoader;

import java.util.concurrent.TimeUnit;

public class GameScreen extends MyScreenAdapter {
    
    private static GameScreen gameScreenInstance = null;
    
    public static long SEED = 4; //test seed
    
    public Engine engine;//, persistenceEngine;
    
    private static long gameTimeCurrent, gameTimeStart, timePaused;
    private boolean isPaused = false;
    
    private boolean inSpace;
    private Entity currentPlanet = null;
    
    
    public static World box2dWorld;
    public static Universe universe;
    public static NoiseManager noiseManager;
    
    private ShaderProgram shader = null;
    
    private SystemsConfig systemsCFG;
    
    
    private GameScreen() {
        
        systemsCFG = SpaceProject.configManager.getConfig(SystemsConfig.class);
        
        
        initUI();
        initShader();
    
        initCore();
    
        //initGame();
    }
    
    private void initUI() {
        //init scene2d/VisUI
        if (VisUI.isLoaded())
            VisUI.dispose(true);
        VisUI.load(SpaceProject.isMobile() ? VisUI.SkinScale.X2 : VisUI.SkinScale.X1);
        BitmapFont font = FontFactory.createFont(FontFactory.fontBitstreamVM, 12);
        VisUI.getSkin().add(FontFactory.skinSmallFont, font);
    }
    
    private void initShader() {
        //playing with shaders
        boolean useShader = false;
        if (useShader) {
            //shader = new ShaderProgram(Gdx.files.internal("shaders/quadRotation.vsh"), Gdx.files.internal("shaders/quadRotation.fsh"));
            //shader = new ShaderProgram(Gdx.files.internal("shaders/passthrough.vsh"), Gdx.files.internal("shaders/passthrough.fsh"));
            //shader = new ShaderProgram(Gdx.files.internal("shaders/invert.vsh"), Gdx.files.internal("shaders/invert.fsh"));
            shader = new ShaderProgram(Gdx.files.internal("shaders/grayscale.vsh"), Gdx.files.internal("shaders/grayscale.fsh"));
            ShaderProgram.pedantic = false;
            Gdx.app.log(this.getClass().getSimpleName(), "Shader compiled: " + shader.isCompiled() + ": " + shader.getLog());
            if (shader.isCompiled())
                batch.setShader(shader);
        }
    }
    
    private void initCore() {
        //ECS
        engine = new Engine();
        
        //physics
        box2dWorld = new World(new Vector2(), true);
        
        //content
        universe = new Universe();
        
        //worker
        EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
        noiseManager = new NoiseManager(engineCFG.maxNoiseGenThreads);
    }
    
    public void initGame(boolean space) {
        this.inSpace = space;
        
        // load test default values
        Entity playerTESTSHIP = EntityFactory.createPlayerShip(0, 0, inSpace);
        
        if (inSpace) {
            initSpace(playerTESTSHIP);
        } else {
            Entity planet = EntityFactory.createPlanet(0, new Entity(), 0, false);
            Gdx.app.log(this.getClass().getSimpleName(), "DEBUG PLANET LOADED");
            
            initWorld(playerTESTSHIP, planet);
        }
        
        gameTimeStart = System.nanoTime();
    }
    
    
    //region system loading
    private void initSpace(Entity transitioningEntity) {
        inSpace = true;
        currentPlanet = null;
        
        SystemLoader.loadSystems(this, engine, inSpace, systemsCFG);
        
        
        //add player
        engine.addEntity(transitioningEntity);
    }
    
    
    private void initWorld(Entity transitioningEntity, Entity planet) {
        inSpace = false;
        currentPlanet = planet;
        
        Misc.printObjectFields(planet.getComponent(SeedComponent.class));
        Misc.printObjectFields(planet.getComponent(PlanetComponent.class));
        //Misc.printEntity(transitionComponent.transitioningEntity);
        
        SystemLoader.loadSystems(this, engine, inSpace, systemsCFG);
        
        
        // add player
        //TODo: this player init stuff is part of transition, should be part of sync / load process
        WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
        int mapSize = planet.getComponent(PlanetComponent.class).mapSize;
        int position = mapSize * worldCFG.tileSize / 2;//set position to middle of planet
        Body body = transitioningEntity.getComponent(PhysicsComponent.class).body;
        body.setTransform(position, position, body.getAngle());
        engine.addEntity(transitioningEntity);
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
        
        if (Gdx.input.isKeyJustPressed(Keys.F1)) {
            //debug
            DebugUtil.printEntities(engine);
        }
        
        if (Gdx.input.isKeyJustPressed(Keys.GRAVE)) {//tilda
            setSystemProcessing(isPaused);
        }
        
    }
    
    public void switchScreen(Entity transEntity, Entity planet) {
        if (Mappers.AI.get(transEntity) != null) {
            Gdx.app.log(this.getClass().getSimpleName(), "REMOVING: " + Misc.objString(transEntity));
            transEntity.add(new RemoveComponent());
			/*//TODO: persist
			// what happens (in terms of persistence) to an entity in process of transitioning?
            //if important -> persist
            //if same world (even if not important) -> persist (land on planet at same time as AI=where is AI)
			if (Mappers.persist.get(e)) {
				System.out.println("MOVED to background engine: " + Misc.objString(e));
				persistenceEngine.addEntity(e);
			}*/
			return;
        }
        
        ImmutableArray<Entity> transitioningEntities = engine.getEntitiesFor(Family.all(ScreenTransitionComponent.class).get());
        ResourceDisposer.disposeAllExcept(engine.getEntities(), transitioningEntities);
        engine.removeAllEntities();//to fix family references when entities added to new engine
    
        ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(transEntity);
        if (inSpace) {
            initWorld(transEntity, planet);
            
            ScreenTransitionSystem.nextStage(screenTrans);
        } else {
            Mappers.screenTrans.get(transEntity).planet = currentPlanet;
            initSpace(transEntity);
    
            ScreenTransitionSystem.nextStage(screenTrans);
        }
        
        
        //adjust physics (no friction in space)
        for (Entity e : transitioningEntities) {
            Body body = e.getComponent(PhysicsComponent.class).body;
            if (inSpace) {
                body.setLinearDamping(0);
                body.setAngularDamping(0);
            } else {
                body.setAngularDamping(30);
                body.setLinearDamping(45);
            }
        }
        
        /*TODO: persist
        for (Entity relevantEntity : backgroundEngine.getEntities()) {
            //if landing on planet, and relevantEntity is on planet, add to engine, remove from backgroundEngine
            //if going to space, and relevantEntity in space, add to engine, remove from backgroundEngine
        }*/
    
        resetCamera();
    }
    
    
    public boolean inSpace() {
        return inSpace;
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
        
        for (EntitySystem system : engine.getSystems()) {
            if (system instanceof IScreenResizeListener) {
                ((IScreenResizeListener) system).resize(width, height);
            }
        }
    }
    
    @Override
    public boolean scrolled(int amount) {
        //TODO: move into hud, hud as input processor
        HUDSystem hud = engine.getSystem(HUDSystem.class);
        if (hud != null) {
            if (hud.getMiniMap().scrolled(amount)) {
                return true;
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
        //if (SpaceProject.isMobile()) {
             setSystemProcessing(false);
       // }
    }
    
    @Override
    public void resume() {
        //called on regain focus
        //if pauseOnLoseFocus, resume
        //if (SpaceProject.isMobile()) {
            setSystemProcessing(true);
        //}
    }
    
    public void setSystemProcessing(boolean process) {
        if (isPaused != process) {
            return;
        }
        
        isPaused = !process;
        Gdx.app.log(this.getClass().getSimpleName(), "paused [" + isPaused + "]");
        
        //adjust time
        if (isPaused) {
            timePaused = System.nanoTime();
        } else {
            long delta = System.nanoTime() - timePaused;
            gameTimeStart += delta;
        }
        
        //enable/disable systems
        for (EntitySystem system : engine.getSystems()) {
            SysCFG sysCFG = systemsCFG.getConfig(system.getClass().getName());
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
        
        box2dWorld.dispose();
        
    }
    
    
    public static GameScreen getInstance() {
        return GameContext.gameContext;
    }
    
    private static class GameContext {
        private static final GameScreen gameContext = new GameScreen();
    }
}

