package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.BarycenterComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.AstroBody;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.generation.noise.NoiseBuffer;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Tile;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class SpaceLoadingSystem extends EntitySystem implements EntityListener {
    
    //private Universe universe;
    private ImmutableArray<Entity> loadedAstronomicalBodies;
    
    
    SimpleTimer checkStarsTimer;//this system might make sense as an interval system instead of timer
    
    
    @Override
    public void addedToEngine(Engine engine) {
        
        // currently loaded stars/planets
        loadedAstronomicalBodies = engine.getEntitiesFor(Family.all(BarycenterComponent.class, TransformComponent.class).get());
        
        engine.addEntityListener(Family.one(PlanetComponent.class, BarycenterComponent.class).get(), this);
        
        
        // load space things (asteroids, wormhole, black hole, etc)
        // load ai/mobs
        initMobs(engine);
        
        checkStarsTimer = new SimpleTimer(4000);
        checkStarsTimer.setCanDoEvent();
    }
    
    
    @Override
    public void entityAdded(Entity entity) {
        PlanetComponent planet = Mappers.planet.get(entity);
        if (planet != null) {
            long seed = Mappers.seed.get(entity).seed;
            GameScreen.noiseManager.loadOrCreateNoiseFor(seed, planet);
        }
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        for (Entity e : getEngine().getEntitiesFor(Family.all(OrbitComponent.class).get())) {
            OrbitComponent orbit = Mappers.orbit.get(e);
            if (orbit.parent != null) {
                if (orbit.parent == entity) {
                    e.add(new RemoveComponent());
                }
            }
        }
    }
    
    
    public void initMobs(Engine engine) {
        //a placeholder to add dummy objects for now
        
        engine.addEntity(EntityFactory.createShip3(-200, 400));
        engine.addEntity(EntityFactory.createShip3(-300, 400));
        engine.addEntity(EntityFactory.createShip3(-400, 400));
        engine.addEntity(EntityFactory.createShip3(-600, 400));
        
        
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
    
    @Override
    public void update(float delta) {
        // load and unload stars
        updateStars();
        
        // update/replace textures
        updatePlanetTextures();
    }
    
    
    private void updatePlanetTextures() {
        //check queue for tilemaps / pixmaps to load into textures
        if (!GameScreen.noiseManager.isNoiseAvailable()) {
            //todo: should be the event instead of the queue (as long as it remains separate threads)
            
            NoiseBuffer noise = GameScreen.noiseManager.getNoiseFromQueue();
            if (noise.pixelatedTileMap == null) {
                Gdx.app.log(this.getClass().getSimpleName(), "ERROR, no map for: [" + noise.seed + "]");
                return;
            }
            
            for (Entity p : getEngine().getEntitiesFor(Family.all(PlanetComponent.class).get())) {
                if (p.getComponent(SeedComponent.class).seed == noise.seed) {
                    // create planet texture from tileMap, replace texture
                    Texture newTex = TextureFactory.generatePlanet(noise.pixelatedTileMap, Tile.defaultTiles);
                    p.getComponent(TextureComponent.class).texture = newTex;
                    Gdx.app.log(this.getClass().getSimpleName(), "Texture loaded: [" + noise.seed + "]");
                    return;
                }
            }
            
            
        }
    }
    
    
    private void updateStars() {
        if (checkStarsTimer.tryEvent()) {
            //distance to check when to load planets
            int loadDistance = (int) SpaceProject.celestcfg.loadSystemDistance;
            loadDistance *= loadDistance;//square for dst2
            
            // remove stars from engine that are too far
            unloadFarEntities(loadDistance);
            
            // add planetary systems to engine
            loadCloseEntities(loadDistance);
        }
    }
    
    private void loadCloseEntities(int loadDistance) {
        for (AstroBody astroBodies : GameScreen.universe.objects) {
            //check if point is close enough to be loaded
            if (Vector2.dst2(astroBodies.x, astroBodies.y, GameScreen.cam.position.x, GameScreen.cam.position.y) < loadDistance) {
                
                // check if astro bodies already in world
                boolean loaded = false;
                for (Entity astroEntity : loadedAstronomicalBodies) {
                    SeedComponent s = Mappers.seed.get(astroEntity);
                    if (s.seed == astroBodies.seed) {
                        loaded = true;
                        break;
                    }
                }
                
                if (!loaded) {
                    for (Entity e : EntityFactory.createAstronomicalObjects(astroBodies.x, astroBodies.y)) {
                        getEngine().addEntity(e);
                    }
                }
                
            }
        }
    }
    
    private void unloadFarEntities(int loadDistance) {
        for (Entity entity : loadedAstronomicalBodies) {
            TransformComponent t = Mappers.transform.get(entity);
            if (Vector2.dst2(t.pos.x, t.pos.y, GameScreen.cam.position.x, GameScreen.cam.position.y) > loadDistance) {
                entity.add(new RemoveComponent());
                Gdx.app.log(this.getClass().getSimpleName(), "Removing Planetary System: " + entity.getComponent(TransformComponent.class).pos.toString());
            }
        }
    }
    
}
