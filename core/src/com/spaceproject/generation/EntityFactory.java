package com.spaceproject.generation;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AstronomicalComponent;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.BarycenterComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.config.RenderOrder;
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
    private static final EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private static final EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private static final CelestialConfig celestCFG = SpaceProject.configManager.getConfig(CelestialConfig.class);
    
    //region characters
    public static Entity createCharacter(float x, float y) {
        Entity entity = new Entity();
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.CHARACTERS.getHierarchy();
        
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
        
        Entity playerShip = createBasicShip(x, y, 0, player, inSpace);
        
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
        
        Entity aiShip = createBasicShip(x, y, 0, ai, inSpace);
        ECSUtil.transferComponent(ai, aiShip, AIComponent.class);
        ECSUtil.transferComponent(ai, aiShip, ControllableComponent.class);
        return aiShip;
    }
    //endregion
    
    
    //region Astronomical / Celestial objects and bodies
    public static Entity createStar(long seed, float x, float y, boolean rotationDir) {
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
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        
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
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        
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
    
    public static Entity createMoon(long seed, Entity parent, float radialDistance, boolean rotationDir) {
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
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        
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
    public static Entity createBasicShip(float x, float y, boolean inSpace) {
        return createBasicShip(x, y, null, inSpace);
    }
    
    public static Entity createBasicShip(float x, float y, Entity driver, boolean inSpace) {
        return createBasicShip(x, y, MyMath.getSeed(x, y), driver, inSpace);
    }
    
    public static Entity createBasicShip(float x, float y, long seed, Entity driver, boolean inSpace) {
        Entity entity = new Entity();
        
        //seed
        MathUtils.random.setSeed(seed);
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        entity.add(seedComp);
        
        
        //transform
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.VEHICLES.getHierarchy();
        transform.rotation = (float) Math.PI / 2; //face upwards
        entity.add(transform);
        
        
        //generate 3D sprite with random even size
        int shipSize = MathUtils.random(entityCFG.shipSizeMin, entityCFG.shipSizeMax) * 2;
        Texture shipTop = TextureFactory.generateShip(seed, shipSize);
        Texture shipBottom = TextureFactory.generateShipUnderSide(shipTop);
        float spriteScale = 0.025f;//TODO: better way to manage render scale (3d vs tex, relation to physics body)
        float width = shipTop.getWidth() * engineCFG.bodyScale;
        float height = shipTop.getHeight() * engineCFG.bodyScale;
        Sprite3DComponent sprite3DComp = new Sprite3DComponent();
        sprite3DComp.renderable = new Sprite3D(shipTop, shipBottom, engineCFG.entityScale);
        sprite3DComp.renderable.scale.set(spriteScale, spriteScale, spriteScale);
        entity.add(sprite3DComp);
        
        
        //collision detection
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createShip(x, y, width, height, entity, inSpace);
        entity.add(physics);
        
        
        //engine data and marks entity as drive-able
        VehicleComponent vehicle = new VehicleComponent();
        vehicle.driver = driver;
        vehicle.thrust = entityCFG.engineThrust;
        entity.add(vehicle);
        
        
        //health
        HealthComponent health = new HealthComponent();
        health.maxHealth = entityCFG.shipHealth;
        health.health = health.maxHealth;
        entity.add(health);
        
        
        //weapon
        if (MathUtils.randomBoolean()) {
            CannonComponent cannon = new CannonComponent();
            cannon.damage = entityCFG.cannonDamage;
            cannon.maxAmmo = entityCFG.cannonAmmo;
            cannon.curAmmo = cannon.maxAmmo;
            cannon.timerFireRate = new SimpleTimer(entityCFG.cannonFireRate);
            cannon.size = entityCFG.cannonSize;
            cannon.velocity = entityCFG.cannonVelocity;
            cannon.acceleration = entityCFG.cannonAcceleration;
            cannon.anchorVec = new Vector2(width, 0);
            cannon.aimAngle = 0;
            cannon.timerRechargeRate = new SimpleTimer(entityCFG.cannonRechargeRate);
            entity.add(cannon);
        } else {
            ChargeCannonComponent chargeCannon = new ChargeCannonComponent();
            chargeCannon.anchorVec = new Vector2(width, 0);
            chargeCannon.aimAngle = 0;
            chargeCannon.velocity = entityCFG.cannonVelocity;
            chargeCannon.maxSize = 0.30f;
            chargeCannon.minSize = 0.1f;
            chargeCannon.growRateTimer = new SimpleTimer(1500);
            chargeCannon.baseDamage = 8f;
            entity.add(chargeCannon);
        }
        
        
        //hyper drive
        HyperDriveComponent hyperDrive = new HyperDriveComponent();
        hyperDrive.speed = entityCFG.hyperSpeed;
        hyperDrive.coolDownTimer = new SimpleTimer(entityCFG.controlTimerHyperCooldown, true);
        hyperDrive.coolDownTimer.setCanDoEvent();
        entity.add(hyperDrive);
        
        
        //shield
        ShieldComponent shield = new ShieldComponent();
        shield.animTimer = new SimpleTimer(300, true);
        shield.defence = 100f;
        float radius = Math.max(MyMath.calculateBoundingBox(physics.body).getWidth(), MyMath.calculateBoundingBox(physics.body).getHeight());
        shield.maxRadius = radius;
        shield.color = Color.BLUE;
        entity.add(shield);
        
        
        //barrel roll
        BarrelRollComponent barrelRoll = new BarrelRollComponent();
        barrelRoll.rollTimer = new SimpleTimer(entityCFG.dodgeTimeout);
        barrelRoll.animationTimer = new SimpleTimer(entityCFG.dodgeAnimationTimer, true);
        barrelRoll.animInterpolation = Interpolation.pow2;
        barrelRoll.revolutions = 1;
        barrelRoll.dir = BarrelRollComponent.FlipDir.none;
        barrelRoll.force = entityCFG.dodgeForce;
        entity.add(barrelRoll);
    
        ParticleComponent particle = new ParticleComponent();
        //particle.particleEffect = new ParticleEffectPool();
        //particle.particleEffect.load(Gdx.files.internal("particles/test.particle"), Gdx.files.internal("particles/"));
        //article.//particleEffect.getEmitters().first().setPosition(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
        //particle.particleEffect.start();
        //particle.offsetY = 1f;
        
        particle.offset = new Vector2(0, height + 0.2f);
        entity.add(particle);
        
        //map
        MapComponent map = new MapComponent();
        map.color = new Color(1, 1, 1, 0.9f);
        map.distance = 3000;
        entity.add(map);
        
        
        return entity;
    }
    //endregion
    
    public static Entity createWall(int x, int y, int width, int height) {
        Entity entity = new Entity();
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureFactory.generateWall(width * engineCFG.pixelPerUnit, height * engineCFG.pixelPerUnit, new Color(0.4f, 0.4f, 0.4f, 1));
        texture.scale = 0.05f;
    
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyFactory.createWall(x, y, width, height, entity);
    
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.WORLD_OBJECTS.getHierarchy();
        
        entity.add(transform);
        entity.add(physics);
        entity.add(texture);
        
        return entity;
    }
}
