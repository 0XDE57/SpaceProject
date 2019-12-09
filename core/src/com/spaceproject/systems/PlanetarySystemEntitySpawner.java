package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.AISpawnComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.SimpleTimer;

public class PlanetarySystemEntitySpawner extends IteratingSystem {
    
    public PlanetarySystemEntitySpawner() {
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
        Entity aiShip = EntityFactory.createAIShip(transform.pos.x, transform.pos.y, GameScreen.inSpace());
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
        getEngine().addEntity(aiShip);
        Gdx.app.log(this.getClass().getSimpleName(), "spawned: " + Misc.objString(entity) + " source:" + Misc.objString(entity));
    
        //if i am another type of object, then do my own spawn rules. like maybe I am on a planet and want to spawn characters near the player
        //TransformComponent transform = Mappers.transform.get(entity);
        //Entity ai = EntityFactory.createCharacterAI(transform.pos.x, transform.pos.y);
        //ai.getComponent(AIComponent.class).State = AIComponent.State.dumbwander;
        //getEngine().addEntity(ai);
    }
}
