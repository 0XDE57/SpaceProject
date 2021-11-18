package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AISpawnComponent;
import com.spaceproject.components.BarycenterComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.generation.AstroBody;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.noise.NoiseBuffer;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Tile;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class SpaceLoadingSystem extends EntitySystem implements EntityListener {
    
    private final CelestialConfig celestCFG = SpaceProject.configManager.getConfig(CelestialConfig.class);
    private ImmutableArray<Entity> loadedAstronomicalBodies;
    private SimpleTimer checkStarsTimer;
    
    private boolean hasInit;
    
    
    @Override
    public void addedToEngine(Engine engine) {
        // currently loaded stars/planets
        loadedAstronomicalBodies = engine.getEntitiesFor(Family.all(BarycenterComponent.class, TransformComponent.class).get());
        
        //engine.addEntityListener(Family.one(PlanetComponent.class, BarycenterComponent.class).get(), this);
        
        
        // load space things (asteroids, wormhole, black hole, etc)
        // load ai/mobs
        //initMobs(engine);
        hasInit = false;
        
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
    
    
    public void initTestDebugMobs(Engine engine) {
        hasInit = true;
        
        //a placeholder to add dummy objects for now
        for (Entity e : EntityFactory.createBasicShip(-20, 40, GameScreen.inSpace())) {
            engine.addEntity(e);
        }
        for (Entity e : EntityFactory.createBasicShip(-30, 40, GameScreen.inSpace())) {
            engine.addEntity(e);
        }
        for (Entity e : EntityFactory.createBasicShip(-40, 40, GameScreen.inSpace())) {
            engine.addEntity(e);
        }
        for (Entity e : EntityFactory.createBasicShip(-60, 40, GameScreen.inSpace())) {
            engine.addEntity(e);
        }
        
        Entity aiTest = EntityFactory.createCharacterAI(0, 40);
        Mappers.AI.get(aiTest).state = AIComponent.State.dumbwander;
        //aiTest.add(new CameraFocusComponent());//test cam focus on AI
        engine.addEntity(aiTest);
        
        Entity aiTest2 = EntityFactory.createCharacterAI(0, 60);
        Mappers.AI.get(aiTest2).state = AIComponent.State.idle;
        engine.addEntity(aiTest2);
        
        Entity aiTest3 = EntityFactory.createCharacterAI(0, 80);
        Mappers.AI.get(aiTest3).state = AIComponent.State.landOnPlanet;
        //aiTest3.add(new CameraFocusComponent());
        engine.addEntity(aiTest3);
    }
    
    @Override
    public void update(float delta) {
        if (!hasInit) {
            initTestDebugMobs(getEngine());
        }
        
        // load and unload stars
        updateStars(celestCFG.loadSystemDistance);
        
        // update/replace textures
        updatePlanetTextures();
    }
    
    
    private void updatePlanetTextures() {
        //todo: this should probably subscribe to a thread finished event instead of polling
        //check queue for tilemaps / pixmaps to load into textures
        if (!GameScreen.noiseManager.isNoiseAvailable()) {
            return;
        }
        
        //grab noise from queue
        NoiseBuffer noise = GameScreen.noiseManager.getNoiseFromQueue();
        if (noise.pixelatedTileMap == null) {
            Gdx.app.log(this.getClass().getSimpleName(), "ERROR, no map for: [" + noise.seed + "]");
            return;
        }
    
        //find planet that noise belongs to (matching seed)
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
    
    
    private void updateStars(float loadDistance) {
        if (checkStarsTimer.tryEvent()) {
            loadDistance *= loadDistance;//square for dst2
            
            // remove stars from engine that are too far
            unloadFarEntities(loadDistance);
            
            // add planetary systems to engine
            loadCloseEntities(loadDistance);
        }
    }
    
    private void loadCloseEntities(float loadDistance) {
        for (AstroBody astroBodies : GameScreen.galaxy.objects) {
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
                    for (Entity e : createAstronomicalObjects(astroBodies.x, astroBodies.y)) {
                        getEngine().addEntity(e);
                    }
                }
                
            }
        }
    }
    
    private void unloadFarEntities(float loadDistance) {
        for (Entity entity : loadedAstronomicalBodies) {
            TransformComponent t = Mappers.transform.get(entity);
            if (Vector2.dst2(t.pos.x, t.pos.y, GameScreen.cam.position.x, GameScreen.cam.position.y) > loadDistance) {
                entity.add(new RemoveComponent());
                Gdx.app.log(this.getClass().getSimpleName(), "Removing Planetary System: " + entity.getComponent(TransformComponent.class).pos.toString());
            }
        }
    }
    
    
    public Array<Entity> createAstronomicalObjects(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        MathUtils.random.setSeed(seed);
        
        switch (MathUtils.random(2)) {
            case 0:
                return createPlanetarySystem(x, y);
            case 1:
                return createBinarySystem(x, y);
            case 2:
                return createRoguePlanet(x, y);
        }
        
        return createPlanetarySystem(x, y);
    }
    
    public Array<Entity> createPlanetarySystem(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createPlanetarySystem(x, y, seed);
    }
    
    public Array<Entity> createPlanetarySystem(float x, float y, long seed) {
        MathUtils.random.setSeed(seed);
        
        //number of planets in a system
        int numPlanets = MathUtils.random(celestCFG.minPlanets, celestCFG.maxPlanets);
        
        //distance between planets
        float distance = celestCFG.minPlanetDist / 3; //add some initial distance between star and first planet
        
        //rotation of system (orbits and spins)
        boolean rotDir = MathUtils.randomBoolean();
        
        //collection of planets/stars
        Array<Entity> entities = new Array<Entity>();
        
        //add star to center of planetary system
        Entity star = EntityFactory.createStar(seed, x, y, rotDir);
        BarycenterComponent barycenter = new BarycenterComponent();
        barycenter.bodyType = numPlanets == 0 ? BarycenterComponent.AstronomicalBodyType.loneStar : BarycenterComponent.AstronomicalBodyType.uniStellar;
        star.add(barycenter);
        entities.add(star);
        
        //create planets around star
        for (int i = 0; i < numPlanets; ++i) {
            //add some distance from previous entity
            distance += MathUtils.random(celestCFG.minPlanetDist, celestCFG.maxPlanetDist);
            
            //create planet
            long planetSeed = MyMath.getSeed(x, y + distance);
            Entity planet = EntityFactory.createPlanet(planetSeed, star, distance, rotDir);
            
            //add moon
            boolean hasMoon = MathUtils.randomBoolean();
            if (hasMoon) {
                float moonDist = planet.getComponent(TextureComponent.class).texture.getWidth() * planet.getComponent(TextureComponent.class).scale * 2;
                moonDist *= 0.7f;
                distance += moonDist;
                Entity moon = EntityFactory.createMoon(MyMath.getSeed(x, y + distance), planet, moonDist, rotDir);
                entities.add(moon);
                
                /*// nested test
                distance += moonDist;
                Entity moonsMoon = EntityFactory.createMoon(MyMath.getSeed(x, y + distance), moon, moonDist, rotDir);
                entities.add(moonsMoon);
    
                distance += moonDist;
                Entity moonsMoonsMoon = EntityFactory.createMoon(MyMath.getSeed(x, y + distance), moonsMoon, moonDist, rotDir);
                entities.add(moonsMoonsMoon);*/
            }
            
            //addLifeToPlanet(planet);
            
            entities.add(planet);
        }
        
        Gdx.app.log(this.getClass().getSimpleName(), "Planetary System: [" + seed + "](" + x + ", " + y + ") Objects: " + (numPlanets));
        
        return entities;
        
    }
    
    private static void addLifeToPlanet(Entity planet) {
        //add entity spawner if planet has life
        //dumb coin flip for now, can have rules later like no life when super close to star = lava, or super far = ice
        //simply base it on distance from star. habital zone
        //  eg chance of life = distance from habit zone
        //more complex rules can be applied like considering the planets type. ocean might have life but if entire planet is ocean = no ships = no spawner
        //desert might have different life, so different spawner rules
        boolean hasLife = MathUtils.randomBoolean();
        if (hasLife) {
            int min = 10000;
            int max = 100000;
            int lifeDensity = MathUtils.random(1);
            
            AISpawnComponent spawnComponent = new AISpawnComponent();
            spawnComponent.min = min;
            spawnComponent.max = max;
            spawnComponent.timers = new SimpleTimer[lifeDensity];
            spawnComponent.state = AIComponent.State.attack;
            for (int t = 0; t < spawnComponent.timers.length; t++) {
                spawnComponent.timers[t] = new SimpleTimer(MathUtils.random(spawnComponent.min, spawnComponent.max));
            }
            planet.add(spawnComponent);
        }
    }
    
    public Array<Entity> createRoguePlanet(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createRoguePlanet(x, y, seed);
    }
    
    public Array<Entity> createRoguePlanet(float x, float y, long seed) {
        MathUtils.random.setSeed(seed);
        Array<Entity> entities = new Array<>();
        
        Entity planet = EntityFactory.createPlanet(seed, null, 0, MathUtils.randomBoolean());
        planet.getComponent(OrbitComponent.class).tangentialSpeed = 0;
        
        BarycenterComponent barycenter = new BarycenterComponent();
        barycenter.bodyType = BarycenterComponent.AstronomicalBodyType.roguePlanet;
        planet.add(barycenter);
        planet.getComponent(TransformComponent.class).pos.set(x, y);
        
        //add moon
        boolean hasMoon = MathUtils.randomBoolean();
        if (hasMoon) {
            float moonDist = planet.getComponent(TextureComponent.class).texture.getWidth() * planet.getComponent(TextureComponent.class).scale * 2;
            boolean rotDir = MathUtils.randomBoolean();
            Entity moon = EntityFactory.createMoon(MyMath.getSeed(x, y + moonDist), planet, moonDist, rotDir);
            entities.add(moon);
        }
        
        entities.add(planet);
        Gdx.app.log(this.getClass().getSimpleName(), "Rogue Planet: [" + seed + "](" + x + ", " + y + ")");
        return entities;
    }
    
    public Array<Entity> createBinarySystem(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createBinarySystem(x, y, seed);
    }
    
    public Array<Entity> createBinarySystem(float x, float y, long seed) {
        Entity anchorEntity = new Entity();
        
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        anchorEntity.add(seedComp);
        
        BarycenterComponent barycenter = new BarycenterComponent();
        barycenter.bodyType = BarycenterComponent.AstronomicalBodyType.multiStellar;
        anchorEntity.add(barycenter);
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        anchorEntity.add(transform);
    
        
        //add stars
        float distance = celestCFG.maxPlanetSize * 2 + celestCFG.maxPlanetDist * 2;
        boolean rotDir = MathUtils.randomBoolean();
        float startAngle = MathUtils.random(MathUtils.PI2);
        float tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        
        Entity starA = EntityFactory.createStar(MyMath.getSeed(x + distance, y), x + distance, y, rotDir);
        OrbitComponent orbitA = starA.getComponent(OrbitComponent.class);
        orbitA.parent = anchorEntity;
        orbitA.radialDistance = distance;
        orbitA.startAngle = startAngle;
        orbitA.tangentialSpeed = tangentialSpeed;
        
        Entity starB = EntityFactory.createStar(MyMath.getSeed(x - distance, y), x - distance, y, rotDir);
        OrbitComponent orbitB = starB.getComponent(OrbitComponent.class);
        orbitB.parent = anchorEntity;
        orbitB.radialDistance = distance;
        orbitB.startAngle = startAngle + MathUtils.PI;
        orbitB.tangentialSpeed = tangentialSpeed;
        
        
        Array<Entity> entities = new Array<Entity>();
        entities.add(anchorEntity);
        entities.add(starA);
        entities.add(starB);
        
        Gdx.app.log(this.getClass().getSimpleName(), "Binary System: [" + seed + "](" + x + ", " + y + ")");
        return entities;
    }
    
}
