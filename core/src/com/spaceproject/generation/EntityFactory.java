package com.spaceproject.generation;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AISpawnComponent;
import com.spaceproject.components.AstronomicalComponent;
import com.spaceproject.components.BarycenterComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.GrowCannonComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Sprite3D;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.SimpleTimer;


public class EntityFactory {
    //todo: refactor into separate factories for entity types
    // eg: planetary system should go into a AstronomicalBodyEntityFactory()
    // ships into ShipEntityFactory
    // rename this to base or common? and keep character
    private static EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private static EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private static CelestialConfig celestCFG = SpaceProject.configManager.getConfig(CelestialConfig.class);
    
    //region characters
    public static Entity createCharacter(float x, float y) {
        Entity entity = new Entity();
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = -100;
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateCharacter();
        texture.scale = 0.1f;
        
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createPlayerBody(x, y, entity);
        
        CharacterComponent character = new CharacterComponent();
        character.walkSpeed = entityCFG.characterWalkSpeed;
        
        HealthComponent health = new HealthComponent();
        health.maxHealth = entityCFG.characterHealth;
        health.health = health.maxHealth;
        
        ControllableComponent control = new ControllableComponent();
        control.timerVehicle = new SimpleTimer(entityCFG.controlTimerVehicle);
        control.timerDodge = new SimpleTimer(entityCFG.controlTimerDodge);
        control.actionACooldownTimer = new SimpleTimer(entityCFG.controlATimerCoolDown);
        
        
        entity.add(health);
        entity.add(control);
        entity.add(physics);
        entity.add(transform);
        entity.add(texture);
        entity.add(character);
        
        return entity;
    }
    
    public static Entity createPlayer(float x, float y) {
        Entity character = createCharacter(x, y);
        CameraFocusComponent cameraFocus = new CameraFocusComponent();
        cameraFocus.zoomTarget = engineCFG.defaultZoomCharacter;
        character.add(cameraFocus);
        character.add(new ControlFocusComponent());
        return character;
    }
    
    public static Entity createCharacterAI(float x, float y) {
        Entity character = createCharacter(x, y);
        character.add(new AIComponent());
        return character;
    }
    
    public static Entity createPlayerShip(int x, int y, boolean inSpace) {
        Entity player = createPlayer(x, y);
        
        PhysicsComponent physicsComponent = player.getComponent(PhysicsComponent.class);
        GameScreen.box2dWorld.destroyBody(physicsComponent.body);
        physicsComponent.body = null;
        
        Entity playerShip = createShip3(x, y, 0, player, inSpace);
        
        ECSUtil.transferComponent(player, playerShip, ControlFocusComponent.class);
        ECSUtil.transferComponent(player, playerShip, ControllableComponent.class);
        CameraFocusComponent cameraFocus = (CameraFocusComponent)ECSUtil.transferComponent(player, playerShip, CameraFocusComponent.class);
        cameraFocus.zoomTarget = engineCFG.defaultZoomVehicle;
        
        return playerShip;
    }
    
    public static Entity createAIShip(float x, float y, boolean inSpace) {
        Entity ai = createCharacterAI(x, y);
        
        PhysicsComponent physicsComponent = ai.getComponent(PhysicsComponent.class);
        GameScreen.box2dWorld.destroyBody(physicsComponent.body);
        physicsComponent.body = null;
        
        Entity aiShip = createShip3(x, y, 0, ai, inSpace);
        ECSUtil.transferComponent(ai, aiShip, AIComponent.class);
        ECSUtil.transferComponent(ai, aiShip, ControllableComponent.class);
        return aiShip;
    }
    //endregion
    
    
    //region Astronomical / Celestial objects and bodies
    public static Array<Entity> createAstronomicalObjects(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        MathUtils.random.setSeed(seed);
        
        switch (MathUtils.random(2)) {
            case 0:
                return createPlanetarySystem(x, y, seed);
            case 1:
                return createBinarySystem(x, y, seed);
            case 2:
                return createRoguePlanet(x, y, seed);
        }
    
        return createPlanetarySystem(x, y, seed);
    }
    
    public static Array<Entity> createPlanetarySystem(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createPlanetarySystem(x, y, seed);
    }
    
    public static Array<Entity> createPlanetarySystem(float x, float y, long seed) {
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
        Entity star = createStar(seed, x, y, rotDir);
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
            Entity planet = createPlanet(planetSeed, star, distance, rotDir);
            
            //add moon
            boolean hasMoon = MathUtils.randomBoolean();
            if (hasMoon) {
                float moonDist = planet.getComponent(TextureComponent.class).texture.getWidth() * planet.getComponent(TextureComponent.class).scale * 2;
                moonDist *= 1.5f;
                distance += moonDist;
                Entity moon = createMoon(MyMath.getSeed(x, y + distance), planet, moonDist, rotDir);
                entities.add(moon);
                
                /* nested test
                distance += moonDist;
                Entity moonsMoon = createMoon(MyMath.getSeed(x, y + distance), moon, moonDist, rotDir);
                entities.add(moonsMoon);
    
                distance += moonDist;
                Entity moonsMoonsMoon = createMoon(MyMath.getSeed(x, y + distance), moonsMoon, moonDist, rotDir);
                entities.add(moonsMoonsMoon);*/
            }
            
            //addLifeToPlanet(planet);
    
            entities.add(planet);
        }
        
        Gdx.app.log(EntityFactory.class.getSimpleName(), "Planetary System: (" + x + ", " + y + ") Objects: " + (numPlanets));
        
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
    
    public static Array<Entity> createBinarySystem(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createBinarySystem(x, y, seed);
    }
    
    public static Array<Entity> createBinarySystem(float x, float y, long seed) {
        Entity anchorEntity = new Entity();
        
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        BarycenterComponent barycenter = new BarycenterComponent();
        barycenter.bodyType = BarycenterComponent.AstronomicalBodyType.multiStellar;
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        anchorEntity.add(transform);
        anchorEntity.add(barycenter);
        anchorEntity.add(seedComp);
        
        
        //distance between planets
        float distance = celestCFG.maxPlanetSize * 2 + celestCFG.maxPlanetDist * 2; //add distance between stars
        
        //rotation of system (orbits and spins)
        boolean rotDir = MathUtils.randomBoolean();
        
        //collection of planets/stars
        Array<Entity> entities = new Array<Entity>();
        
        //add stars
        float startAngle = MathUtils.random(MathUtils.PI2);
        float tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        Entity starA = createStar(MyMath.getSeed(x + distance, y), x + distance, y, rotDir);
        OrbitComponent orbitA = starA.getComponent(OrbitComponent.class);
        orbitA.parent = anchorEntity;
        orbitA.radialDistance = distance;
        orbitA.startAngle = startAngle;
        orbitA.tangentialSpeed = tangentialSpeed;
        
        Entity starB = createStar(MyMath.getSeed(x - distance, y), x - distance, y, rotDir);
        OrbitComponent orbitB = starB.getComponent(OrbitComponent.class);
        orbitB.parent = anchorEntity;
        orbitB.radialDistance = distance;
        orbitB.startAngle = startAngle + MathUtils.PI;
        orbitB.tangentialSpeed = tangentialSpeed;
        
        
        entities.add(anchorEntity);
        entities.add(starA);
        entities.add(starB);
        
        Gdx.app.log(EntityFactory.class.getSimpleName(), "Binary System: (" + x + ", " + y + ")");
        return entities;
    }
    
    private static Entity createStar(long seed, float x, float y, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        
        Entity entity = new Entity();
        SeedComponent seedComponent = new SeedComponent();
        seedComponent.seed = seed;
        
        AstronomicalComponent astro = new AstronomicalComponent();
        astro.classification = AstronomicalComponent.Classification.star;
        
        // create star texture
        TextureComponent texture = new TextureComponent();
        int radius = MathUtils.random(celestCFG.minStarSize, celestCFG.maxStarSize);
        texture.texture = TextureFactory.generateStar(seed, radius);
        texture.scale = engineCFG.entityScale;
        
        // set position
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = 50;
        
        //orbit for rotation of self (kinda hacky; not really orbiting, just rotating)
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = null;//set to null to negate orbit, but keep rotation
        orbit.rotateClockwise = rotationDir;
        orbit.rotSpeed = MathUtils.random(celestCFG.minStarRot, celestCFG.maxStarRot); //rotation speed of star
        
        //mapState
        MapComponent map = new MapComponent();
        map.color = new Color(0.9f, 0.9f, 0.15f, 0.9f);
        map.distance = 80000;
        
        
        //add components to entity
        entity.add(seedComponent);
        entity.add(astro);
        entity.add(orbit);
        entity.add(map);
        entity.add(transform);
        entity.add(texture);
        return entity;
    }
    
    public static Array<Entity> createRoguePlanet(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createRoguePlanet(x, y, seed);
    }
    
    public static Array<Entity> createRoguePlanet(float x, float y, long seed) {
        Array<Entity> entities = new Array<>();
        
        MathUtils.random.setSeed(seed);
        
        BarycenterComponent barycenter = new BarycenterComponent();
        barycenter.bodyType = BarycenterComponent.AstronomicalBodyType.roguePlanet;
        
        Entity planet = createPlanet(seed, null, 0, MathUtils.randomBoolean());
        planet.add(barycenter);
        planet.getComponent(TransformComponent.class).pos.set(x, y);
    
        //add moon
        boolean hasMoon = MathUtils.randomBoolean();
        if (hasMoon) {
            float moonDist = planet.getComponent(TextureComponent.class).texture.getWidth() * planet.getComponent(TextureComponent.class).scale * 2;
            boolean rotDir = MathUtils.randomBoolean();
            Entity moon = createMoon(MyMath.getSeed(x, y + moonDist), planet, moonDist, rotDir);
            entities.add(moon);
        }
        
        entities.add(planet);
        Gdx.app.log(EntityFactory.class.getSimpleName(), "Rogue Planet: (" + x + ", " + y + ")");
        return entities;
    }
    
    public static Entity createPlanet(long seed, Entity parent, float radialDistance, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        
        Entity entity = new Entity();
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        
        AstronomicalComponent astro = new AstronomicalComponent();
        astro.classification = AstronomicalComponent.Classification.planet;
    
        int planetSize = (int) Math.pow(2, MathUtils.random(7, 10));
        
        //create placeholder texture. real texture will be generated by a thread
        TextureComponent texture = new TextureComponent();
        int chunkSize = SpaceProject.configManager.getConfig(WorldConfig.class).chunkSize;
        texture.texture = TextureFactory.generatePlanet(planetSize, chunkSize);
        texture.scale = 16;
        
        TransformComponent transform = new TransformComponent();
        transform.zOrder = 50;
        
        //orbit
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = parent;
        orbit.radialDistance = radialDistance;
        orbit.tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        orbit.startAngle = MathUtils.random(MathUtils.PI2);
        orbit.rotSpeed = MathUtils.random(celestCFG.minPlanetRot, celestCFG.maxPlanetRot);
        orbit.rotateClockwise = rotationDir;
        
        //map
        MapComponent map = new MapComponent();
        map.color = new Color(0.15f, 0.5f, 0.9f, 0.9f);
        map.distance = 10000;
        
        //planet
        PlanetComponent planet = new PlanetComponent();
        planet.mapSize = planetSize;
        //TODO: randomize features/load from feature profile
        planet.scale = 100;
        planet.octaves = 4;
        planet.persistence = 0.68f;
        planet.lacunarity = 2.6f;
        
        //add components to entity
        entity.add(seedComp);
        entity.add(astro);
        entity.add(planet);
        entity.add(orbit);
        entity.add(map);
        entity.add(transform);
        entity.add(texture);
        return entity;
    }
    
    private static Entity createMoon(long seed, Entity parent, float radialDistance, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        
        Entity entity = new Entity();
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        
        AstronomicalComponent astro = new AstronomicalComponent();
        astro.classification = AstronomicalComponent.Classification.moon;
        
        //create placeholder texture. real texture will be generated by a thread
        TextureComponent texture = new TextureComponent();
        int size = (int) Math.pow(2, MathUtils.random(5, 7));
        int chunkSize = SpaceProject.configManager.getConfig(WorldConfig.class).chunkSize;
        texture.texture = TextureFactory.generatePlanet(size, chunkSize);
        texture.scale = 16;
        
        TransformComponent transform = new TransformComponent();
        transform.zOrder = 50;
        
        //orbit
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = parent;
        orbit.radialDistance = radialDistance;
        orbit.tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        orbit.startAngle = MathUtils.random(MathUtils.PI2);
        orbit.rotSpeed = MathUtils.random(celestCFG.minPlanetRot, celestCFG.maxPlanetRot);
        orbit.rotateClockwise = rotationDir;
        
        //map
        MapComponent map = new MapComponent();
        map.color = new Color(0.5f, 0.6f, 0.6f, 0.9f);
        map.distance = 10000;
        
        
        //add components to entity
        entity.add(seedComp);
        entity.add(astro);
        entity.add(orbit);
        entity.add(map);
        entity.add(transform);
        entity.add(texture);
        return entity;
    }
    //endregion
    
    
    //region ships
    public static Entity createShip3(float x, float y, boolean inSpace) {
        return createShip3(x, y, null, inSpace);
    }
    
    public static Entity createShip3(float x, float y, Entity driver, boolean inSpace) {
        return createShip3(x, y, MyMath.getSeed(x, y), driver, inSpace);
    }
    
    public static Entity createShip3(float x, float y, long seed, Entity driver, boolean inSpace) {
        Entity entity = new Entity();
        
        MathUtils.random.setSeed(seed);
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        
        //transform
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = -10;
        transform.rotation = (float) Math.PI / 2; //face upwards
        
        //generate random even size
        int shipSize = MathUtils.random(entityCFG.shipSizeMin, entityCFG.shipSizeMax) * 2;
        Texture shipTop = TextureFactory.generateShip(seed, shipSize);
        Texture shipBottom = TextureFactory.generateShipUnderSide(shipTop);
        
        float spriteScale = 0.025f;//TODO: better way to manage render scale (3d vs tex, relation to physics body)
        float width = shipTop.getWidth() * engineCFG.bodyScale;
        float height = shipTop.getHeight() * engineCFG.bodyScale;
        
        Sprite3DComponent sprite3DComp = new Sprite3DComponent();
        sprite3DComp.renderable = new Sprite3D(shipTop, shipBottom, engineCFG.entityScale);
        sprite3DComp.renderable.scale.set(spriteScale, spriteScale, spriteScale);
        
        //collision detection
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createShip(x, y, width, height, entity, inSpace);
        
        //weapon
        CannonComponent cannon = new CannonComponent();
        cannon.damage = entityCFG.cannonDamage;
        cannon.maxAmmo = entityCFG.cannonAmmo;
        cannon.curAmmo = cannon.maxAmmo;
        cannon.timerFireRate = new SimpleTimer(entityCFG.cannonFireRate);//lower is faster
        cannon.size = entityCFG.cannonSize; //higher is bigger
        cannon.velocity = entityCFG.cannonVelocity; //higher is faster
        cannon.acceleration = entityCFG.cannonAcceleration;
        cannon.anchorVec = new Vector2(width/2+0.2f, 0);
        cannon.timerRechargeRate = new SimpleTimer(entityCFG.cannonRechargeRate);//lower is faster
        
        
        //engine data and marks entity as drive-able
        VehicleComponent vehicle = new VehicleComponent();
        vehicle.driver = driver;
        vehicle.hyperSpeed = entityCFG.hyperVelocity;
        vehicle.thrust = entityCFG.engineThrust;//higher is faster
        vehicle.maxSpeed = vehicle.NOLIMIT;
        
        
        //health
        HealthComponent health = new HealthComponent();
        health.maxHealth = entityCFG.shipHealth;
        health.health = health.maxHealth;
        
        //map
        MapComponent map = new MapComponent();
        map.color = new Color(1, 1, 1, 0.9f);
        map.distance = 3000;
        
        
        //add components to entity
        entity.add(seedComp);
        entity.add(health);
        entity.add(cannon);
        entity.add(physics);
        entity.add(sprite3DComp);
        entity.add(transform);
        entity.add(vehicle);
        entity.add(map);
        return entity;
    }
    
    public static Entity createShip3Test(float x, float y, long seed, Entity driver, boolean inSpace) {
        Entity ship = createShip3(x, y, seed, driver, inSpace);
        ship.remove(CannonComponent.class);
        
        GrowCannonComponent growCannon = new GrowCannonComponent();
        growCannon.velocity = entityCFG.cannonVelocity;
        growCannon.maxSize = 6f;
        growCannon.size = 1f;
        growCannon.growRateTimer = new SimpleTimer(2000);
        growCannon.baseDamage = 8f;
        ship.add(growCannon);
        
        return ship;
    }
    
    //endregion
    
    
    public static Entity createMissile(TransformComponent sourceTransform, CannonComponent cannon, Entity parentEntity) {
        Entity entity = new Entity();
        
        //create texture
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateProjectile();
        texture.scale = engineCFG.bodyScale;
        
        
        //physics
        PhysicsComponent physics = new PhysicsComponent();
        float bodyWidth = texture.texture.getWidth() * engineCFG.bodyScale;
        float bodyHeight = texture.texture.getHeight() * engineCFG.bodyScale;
        Vector2 spawnPos = sourceTransform.pos.add(cannon.anchorVec);
        Vector2 sourceVel = parentEntity.getComponent(PhysicsComponent.class).body.getLinearVelocity();
        Vector2 projectileVel = MyMath.vector(cannon.aimAngle, cannon.velocity).add(sourceVel);
        physics.body = BodyFactory.createRect(spawnPos.x, spawnPos.y, bodyWidth, bodyHeight, BodyDef.BodyType.DynamicBody);
        physics.body.setTransform(spawnPos, sourceTransform.rotation);
        physics.body.setLinearVelocity(projectileVel);
        physics.body.setBullet(true);//turn on CCD
        physics.body.setUserData(entity);
        
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(physics.body.getPosition());
        transform.rotation = physics.body.getAngle();
        transform.zOrder = -9;//in front of background objects(eg: planets, tiles), behind collide-able objects (eg: players, vehicles)
        
        //set expire time
        ExpireComponent expire = new ExpireComponent();
        expire.time = 5;//in seconds ~approx
        
        //missile damage
        DamageComponent missile = new DamageComponent();
        missile.damage = cannon.damage;
        missile.source = parentEntity;
        
        
        entity.add(missile);
        entity.add(expire);
        entity.add(texture);
        entity.add(physics);
        entity.add(transform);
        
        return entity;
    }

    
    public static Entity createWall(int x, int y, int width, int height) {
        Entity entity = new Entity();
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateWall(width * engineCFG.pixelPerUnit, height * engineCFG.pixelPerUnit, new Color(0.4f, 0.4f, 0.4f, 1));
        texture.scale = 0.05f;
    
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createWall(x, y, width, height, entity);
    
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        
        entity.add(transform);
        entity.add(physics);
        entity.add(texture);
        
        return entity;
    }
}
