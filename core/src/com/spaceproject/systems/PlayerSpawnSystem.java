package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;

import static com.spaceproject.screens.MyScreenAdapter.cam;

public class PlayerSpawnSystem extends EntitySystem implements EntityListener {
    
    //  new game:
    //  > start at space station
    //      > which one? closest to 0, 0? set starter spawn
    //      > what if no space station found?
    //      > spawn space station at nearest star?
    //      > what if no star found? nearest planet or any body?
    //      > failsafe: force spawn 0, 0
    //  > set last spawn station to selected stater station
    //  > auto-spawn at starter station
    //  > anytime land, set current spawn to station
    //
    //  die:
    //  > [DESTROYED]
    //  > press [any key] to respawn
    //  > respawn at last used space station: ei: "checkpoint"
    //  ExplodeEntity
    //      transfer cam focus player -> explode
    //      explode particle fx
    //      outward ring effect
    //      ReSpawnComponent, given to explosion entity on death
    //          .reason = stars are hot, asteroids are solid, killed by [entity]
    //          waits for keypress to restart
    //  on respawn keypress, camera pans from explode position to spawn location
    //      explode entity removed.
    //      then respawn starter ship docked at space station. lose all cargo, credits and upgrades.
    //      perhaps all or a % of cargo can be retrieved from explosion location?
    //
    //  take off planet
    //  > spawn at planet
    //      > which planet? load universe. find saved planet
    //
    //  land on planet -> spawn somewhere on planet
    //      > where on planet? don't want to land in water or lava (unless ship floats in water?)
    //
    
    private ImmutableArray<Entity> spaceStations;
    
    @Override
    public void update(float deltaTime) {
        //first spawn, nearest spacestation to 0, 0
        //if (players.count == 0) spawn() //...?
    
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            Vector2 camPos = new Vector2(cam.position.x, cam.position.y);
            Entity spaceStation = ECSUtil.closestEntity(camPos, spaceStations);
            if (spaceStation == null) {
                return;
            }
            Vector2 pos = Mappers.physics.get(spaceStation).body.getPosition();
            //todo: pos.add(dockedPortOffset)
            Array<Entity> playerShip = EntityBuilder.createPlayerShip(pos.x, pos.y, true);
            for (Entity entity : playerShip) {
                getEngine().addEntity(entity);
            }
            Mappers.spaceStation.get(spaceStation).dockedPortA = playerShip.first();
        }
    }
    
    @Override
    public void entityAdded(Entity entity) {
        //could simply hook here? any time a player is added...
        //no because spawn location needs to be correct at time of spawn
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        //if (Mappers.controllable.get(entity) == null) {
        //    return;
        //}
        
        /*
        Gdx.app.log(getClass().getSimpleName(), "Controlled entity assumed to be player; respawning...");
        
        if (GameScreen.inSpace()) {
            //get last space station
            
            Array<Entity> newPlayer = EntityBuilder.createPlayerShip(0, 0, true);
            for (Entity e : newPlayer) {
                getEngine().addEntity(e);
            }
        } else {
            WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
            int mapSize = GameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
            int position = mapSize * worldCFG.tileSize / 2;//set  position to middle of planet
            Entity newPlayer = EntityBuilder.createPlayer(position, position);
            getEngine().addEntity(newPlayer);
        }*/
    }
  
}
