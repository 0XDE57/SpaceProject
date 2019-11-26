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
        //goal: when in a solar system (loaded planets
        //get all plaenet entitys
        //if planet has life
    
        //add to planet SpawnerComponent
        //add spawning rules
        //eg:
        //spawn chance of xxxx everytick
        //lets say spawn timer = 30 seconds, 50% spawn chance
        //or timeer.setInterval(10 seconds from now, and
        //once timer / spawner satisfied
        //
        //could have a more busy planet with more spawners
        
        AISpawnComponent spawn = Mappers.spawn.get(entity);
        for (SimpleTimer timer : spawn.timers) {
            if (timer.canDoEvent()) {
                long nextInterval = MathUtils.random(spawn.min, spawn.max);
                timer.setInterval(nextInterval, true);//also reset
                
                
                if (spawn.state == AIComponent.State.attack) {
                    //spawn ship at planets location
                    TransformComponent transform = Mappers.transform.get(entity);
                    Entity aiShip = EntityFactory.createAIShip(transform.pos.x, transform.pos.y, GameScreen.inSpace());
                    AIComponent aiComponent = aiShip.getComponent(AIComponent.class);
                    aiComponent.state = spawn.state;
                    //find player entity, send angry ships at them
                    ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(ControlFocusComponent.class).get());
                    if (players.size() == 0) {
                        continue;
                    }
                    Entity attackTarget = players.first();
                    aiComponent.attackTarget = attackTarget;
    
                    getEngine().addEntity(aiShip);
                    Gdx.app.log(this.getClass().getSimpleName(), "spawned: " + Misc.objString(entity) + " source:" + Misc.objString(entity));
                }
                
                
                //if i am another type of object, then do my own spawn rules. like maybe I am on a planet and want to spawn characters near the player
                //TransformComponent transform = Mappers.transform.get(entity);
                //Entity ai = EntityFactory.createCharacterAI(transform.pos.x, transform.pos.y);
                //ai.getComponent(AIComponent.class).State = AIComponent.State.dumbwander;
                //getEngine().addEntity(ai);
            }
        }
    }
    
    
    
}
