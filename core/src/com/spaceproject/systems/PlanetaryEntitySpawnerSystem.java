package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AISpawnComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class PlanetaryEntitySpawnerSystem extends IteratingSystem implements EntityListener {
    
    public PlanetaryEntitySpawnerSystem() {
        super(Family.all(AISpawnComponent.class, TransformComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        //goal: give life to the universe
        //when in a planetary system (loaded planets)
        //if planet has life
        //  add to planet SpawnerComponent
        //  add spawning rules
        //possible rules:
        //  ChanceSpawner: chance to spawn on interval
        //      eg: 30 seconds interval, 50% spawn chance
        //  IntervalSpawner: spawn x every x seconds
        //  RandomIntervalSpawner: spawn x between interval(min,max)
        //could have a more busy planet with more spawners / lower interval
        //should consider spawn caps. we don't want to spawn too many,
        //so should consider local count for how many spawner has spawned (entity tracking list?)
        //could have spawnCap for spawner
        //should also consider global entity count, we don't want to just endlessly fill
        //eg: within this system, don't spawn more than 30?
        
        AISpawnComponent spawn = Mappers.spawn.get(entity);
        for (SimpleTimer timer : spawn.timers) {
            if (timer.canDoEvent()) {
                //reset to next interval
                long nextInterval = MathUtils.random(spawn.min, spawn.max);
                timer.setInterval(nextInterval, true);
                
                spawn(entity, spawn);
            }
        }
    }
    
    private void spawn(Entity entity, AISpawnComponent spawn) {
        //spawn ship at planets location
        TransformComponent transform = Mappers.transform.get(entity);
        Array<Entity> aiShipCluster = EntityFactory.createAIShip(transform.pos.x, transform.pos.y, GameScreen.inSpace());
        Entity aiShip = aiShipCluster.first();
        
        AIComponent aiComponent = aiShip.getComponent(AIComponent.class);
        aiComponent.state = spawn.state;
        switch (aiComponent.state) {
            case attack: {
                //find player entity, send angry ships at them
                ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(ControlFocusComponent.class).get());
                if (players.size() == 0) {
                    return;
                }
                Entity attackTarget = players.first();
                aiComponent.attackTarget = attackTarget;
            }
            break;
            case landOnPlanet: {
                //todo: find a planet that is not the one we spawned on
                //set it as target. go land.
                ImmutableArray<Entity> planets = getEngine().getEntitiesFor(Family.all(PlanetComponent.class).get());
                Entity destinationPlanet = planets.random();
                if (destinationPlanet != null) {
                    aiComponent.planetTarget = destinationPlanet;
                }
                //maybe the AI wants to go to another system even
                //eg: % chance destination is another start system outside current
                //and % chance random direction
            }
        }
        
        spawn.spawnCount++;
        for (Entity ent : aiShipCluster) {
            getEngine().addEntity(ent);
        }
        Gdx.app.log(this.getClass().getSimpleName(), "spawned: " + DebugUtil.objString(entity) + " source:" + DebugUtil.objString(entity));
    
        //if i am another type of object, then do my own spawn rules. like maybe I am on a planet and want to spawn characters near the player
        //TransformComponent transform = Mappers.transform.get(entity);
        //Entity ai = EntityFactory.createCharacterAI(transform.pos.x, transform.pos.y);
        //ai.getComponent(AIComponent.class).State = AIComponent.State.dumbwander;
        //getEngine().addEntity(ai);
    }
    

    @Override
    public void entityAdded(Entity entity) {
        if (entity.getComponent(PlanetComponent.class) != null) {
            addLifeToPlanet(entity);
        }
    }
    
    @Override
    public void entityRemoved(Entity entity) {
    
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
}
