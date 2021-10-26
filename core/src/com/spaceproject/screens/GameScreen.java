package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.SysCFG;
import com.spaceproject.config.SystemsConfig;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.generation.Galaxy;
import com.spaceproject.generation.noise.NoiseManager;
import com.spaceproject.math.MyMath;
import com.spaceproject.systems.ScreenTransitionSystem;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.IScreenResizeListener;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.ResourceDisposer;
import com.spaceproject.utility.SystemLoader;

import java.util.concurrent.TimeUnit;

public class GameScreen extends MyScreenAdapter {
    
    //core
    private static Engine engine;
    public static World box2dWorld;
    public static NoiseManager noiseManager;
    
    private static long gameTimeCurrent, gameTimeStart, timePaused;
    private boolean isPaused = false;
    
    private static boolean inSpace;
    private static Entity currentPlanet;
    
    private static long galaxySeed;
    public static Galaxy galaxy;
    
    private static Stage stage;
    
    public static boolean isDebugMode = true;
    
    public GameScreen() {
        //LOG_NONE: mutes all logging.
        //LOG_DEBUG: logs all messages.
        //LOG_ERROR: logs only error messages.
        //LOG_INFO: logs error and normal messages.
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        //Gdx.app.setLogLevel(Application.LOG_INFO);
        
        
        initUI();
        
        initCore();
    
        initGame(true);
    }
    
    private void initUI() {
        //init scene2d/VisUI
        if (VisUI.isLoaded())
            VisUI.dispose(true);
        VisUI.load(SpaceProject.isMobile() ? VisUI.SkinScale.X2 : VisUI.SkinScale.X1);
        BitmapFont font = FontFactory.createFont(FontFactory.fontBitstreamVM, 12);
        VisUI.getSkin().add(FontFactory.skinSmallFont, font);
    
        stage = new Stage(new ScreenViewport());
        getInputMultiplexer().addProcessor(0, stage);
    }
    
    private void initCore() {
        //ECS
        engine = new Engine();
        
        //physics
        box2dWorld = new World(new Vector2(), true);
        
        //worker
        if (noiseManager == null) {
            EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
            noiseManager = new NoiseManager(engineCFG.maxNoiseGenThreads);
        }
    }
    
    private void initGame(boolean space) {
        inSpace = space;
        
        //init content and entities
        galaxySeed = MyMath.getNewGalaxySeed();
        galaxy = new Galaxy();
    
        Array<Entity> playerShipCluster = EntityFactory.createPlayerShip(0, 0, inSpace);
        //init systems
        if (inSpace) {
            initSpace(playerShipCluster);
        } else {
            Entity planet = EntityFactory.createPlanet(0, new Entity(), 0, false);
            Gdx.app.log(this.getClass().getSimpleName(), "DEBUG PLANET LOADED");
            
            initWorld(playerShipCluster, planet);
        }
        
        gameTimeStart = System.nanoTime();
    }
    
    //region system loading
    private void initSpace(Array<Entity> transitioningEntityCluster) {
        inSpace = true;
        
        SystemsConfig systemsCFG = SpaceProject.configManager.getConfig(SystemsConfig.class);
        SystemLoader.loadSystems(this, engine, inSpace, systemsCFG);
        
        if (currentPlanet != null) {
            Vector2 position = Mappers.transform.get(currentPlanet).pos;
            Entity transitioningEntity = transitioningEntityCluster.first();
            Body body = transitioningEntity.getComponent(PhysicsComponent.class).body;
            body.setTransform(position.x, position.y, body.getAngle());
        }
        for (Entity entity : transitioningEntityCluster) {
            engine.addEntity(entity);
        }
        adjustPhysics(transitioningEntityCluster);
    
        currentPlanet = null;
    }
    
    private void initWorld(Array<Entity> transitioningEntityCluster, Entity planet) {
        inSpace = false;
        currentPlanet = planet;
        
        Entity transitioningEntity = transitioningEntityCluster.first();
        Gdx.app.log(this.getClass().getSimpleName(), "Landing " + Misc.objString(transitioningEntity) + " on planet " + Misc.objString(planet));
    
        //load/unload relevant systems
        SystemsConfig systemsCFG = SpaceProject.configManager.getConfig(SystemsConfig.class);
        SystemLoader.loadSystems(this, engine, inSpace, systemsCFG);
        
        // add player
        WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
        int mapSize = planet.getComponent(PlanetComponent.class).mapSize;
        int position = mapSize * worldCFG.tileSize / 2;//set position to middle of planet
        Body body = transitioningEntity.getComponent(PhysicsComponent.class).body;
        body.setTransform(position, position, body.getAngle());
        for (Entity entity : transitioningEntityCluster) {
            engine.addEntity(entity);
        }
    
        adjustPhysics(transitioningEntityCluster);
    }
    
    public void switchScreen(Entity transEntity, Entity planet) {
        Array<Entity> transEntityCluster = ECSUtil.getAttachedEntities(engine, transEntity);
        
        //if AI, remove it
        if (Mappers.AI.get(transEntity) != null) {
            Gdx.app.log(this.getClass().getSimpleName(), "REMOVING AI: " + Misc.objString(transEntity));
            for (Entity e : transEntityCluster) {
                e.add(new RemoveComponent());
            }
            return;
        }
        
        //clean up resources
        ResourceDisposer.disposeAllExcept(engine.getEntities(), transEntityCluster);
        engine.removeAllEntities();//to fix family references when entities added to new engine
        
        ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(transEntity);
        
        //load/unload relevant systems
        if (inSpace) {
            initWorld(transEntityCluster, planet);
        } else {
            screenTrans.planet = currentPlanet;
            initSpace(transEntityCluster);
        }
        
        ScreenTransitionSystem.nextStage(screenTrans);
        
        resetCamera();
    }
    
    private void adjustPhysics(Array<Entity> entities) {
        for (Entity entity : entities) {
            PhysicsComponent physicsComponent = Mappers.physics.get(entity);
            if (physicsComponent != null) {
                Body body = physicsComponent.body;
                if (inSpace) {
                    //no friction in space
                    body.setLinearDamping(0);
                    body.setAngularDamping(0);
                } else {
                    //todo: move to values to engine/world config
                    body.setAngularDamping(30);
                    body.setLinearDamping(45);
                }
            }
        }
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
        
        
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }
    
    //region states
    public static long getGalaxySeed() {
        return galaxySeed;
    }
    
    public static long getPlanetSeed() {
        return Mappers.seed.get(currentPlanet).seed;
    }
    
    public static Entity getCurrentPlanet() {
        return currentPlanet;
    }
    
    public static boolean inSpace() {
        return inSpace;
    }
    
    public static long getGameTimeCurrent() {
        return gameTimeCurrent;
    }
    
    public static void adjustGameTime(long deltaMillis) {
        gameTimeStart += deltaMillis * 1000000;
    }
    
    public static Stage getStage() {
        return stage;
    }
    //endregion
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        
        stage.getViewport().update(width, height, true);
        
        for (EntitySystem system : engine.getSystems()) {
            if (system instanceof IScreenResizeListener) {
                ((IScreenResizeListener) system).resize(width, height);
            }
        }
    }
    
    //region pause/resume
    @Override
    public void pause() {
        setSystemProcessing(false);
    }
    
    @Override
    public void resume() {
        setSystemProcessing(true);
    }
    
    private void setSystemProcessing(boolean process) {
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
        SystemsConfig systemsCFG = SpaceProject.configManager.getConfig(SystemsConfig.class);
        for (EntitySystem system : engine.getSystems()) {
            SysCFG sysCFG = systemsCFG.getConfig(system);
            if (sysCFG.isHaltOnGamePause()) {
                system.setProcessing(!isPaused);
                Gdx.app.log(this.getClass().getSimpleName(), "processing " + (isPaused ? "disabled" : "enabled") + " for " + system.getClass().getSimpleName());
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
            if (sys instanceof Disposable) {
                Gdx.app.log(this.getClass().getSimpleName(), "Disposing: " + sys.getClass().getSimpleName());
                ((Disposable) sys).dispose();
            }
        }
        
        ResourceDisposer.disposeAll(engine.getEntities());
        
        engine.removeAllEntities();
        engine = null;
        
        box2dWorld.dispose();
        
        //getInputMultiplexer().clear();
        galaxy = null;
        noiseManager.dispose();
        noiseManager = null;
        
        stage.dispose();
    }
    
}

