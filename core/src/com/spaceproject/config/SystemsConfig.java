package com.spaceproject.config;


import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.spaceproject.systems.*;

import java.util.ArrayList;

public class SystemsConfig extends Config {
    
    ArrayList<SysCFG> systems;
    
    @Override
    public void loadDefault() {
        //SysCFG(entitySystem, priority, haltOnGamePause, loadInSpace, loadInWorld, loadOnDesktop, loadOnMobile)
        // sorted by priority. with gaps for easy insertion of new systems.
        // flags (handled by @SystemLoader and @GameScreen):
        // - priority: order of execution. order is important for logic and rendering.
        // - haltOnGamePause: if true, system stops processing when game is paused.
        // - loadInSpace: if true, system will be loaded when player is in space.
        // - loadInWorld: if true, system will be loaded when player is on a planet.
        // - loadOnDesktop: if true, will be loaded on desktop platform.
        // - loadOnMobile: if true, will be loaded on iOS or Android platform.
        
        systems = new ArrayList<>();
        systems.add(new SysCFG(ClearScreenSystem.class, 0, false, true, true, true, true));
        //RespawnSystem
        
        //---input---
        systems.add(new SysCFG(DesktopInputSystem.class, 10, true, true, true, true, false));
        systems.add(new SysCFG(ControllerInputSystem.class, 15, true, true, true, true, true));
        systems.add(new SysCFG(MobileInputSystem.class, 20, true, true, true, false, true));
        systems.add(new SysCFG(AISystem.class, 30, true, true, true, true, true));
        
        //---logic---
        systems.add(new SysCFG(CharacterControlSystem.class, 40, true, true, true, true, true));
        
        systems.add(new SysCFG(ShipControlSystem.class, 50, true, true, true, true, true));
        systems.add(new SysCFG(HyperDriveSystem.class, 55, true, true, true, true, true));
        systems.add(new SysCFG(FixedPhysicsSystem.class, 60, true, true, true, true, true));
        systems.add(new SysCFG(CannonSystem.class, 64, true, true, true, true, true));
        systems.add(new SysCFG(ChargeCannonSystem.class, 65, true, true, true, true, true));
        systems.add(new SysCFG(ShieldSystem.class, 66, true, true, true, true, true));
        systems.add(new SysCFG(BarrelRollSystem.class, 69, true, true, true, true, true));
        
        
        systems.add(new SysCFG(OrbitSystem.class, 70, true, true, false, true, true));
        systems.add(new SysCFG(WorldWrapSystem.class, 80, true, false, true, true, true));
    
        //should probably be loading in logic section
        systems.add(new SysCFG(WorldLoadingSystem.class, 82, true, false, true, true, true));
        systems.add(new SysCFG(SpaceLoadingSystem.class, 83, false, true, false, true, true));
        //systems.add(new SysCFG(PlanetarySystemEntitySpawner.class, 160, true, true, false, true, true));
        
        
        //----render----
        systems.add(new SysCFG(SpaceParallaxSystem.class, 85, true, true, false, true, true));
        
        //todo: split into particle update and particle render (particles should render on top of sprites, but updates logic before camera update to fix drift)
        //could have multilayer rendering if needed in rendering pipeline: particle layer pre sprite, particle layer post sprite
        systems.add(new SysCFG(ParticleSystem.class, 86, true, true, true, true, true));
        //particleUpdate
        systems.add(new SysCFG(CameraSystem.class, 90, false, true, true, true, true));
        //particleRender
        
        //todo: rendering pipeline
        // - screen clear
        // - particle logic update
        // - cam update
        // - astroid renderer -> shape render pre sprite
        // - sprite 2D renderer
        // - sprite 3D renderer
        // - shield renderer -> shape render post sprite
        // - particle renderer -> post sprite
        systems.add(new SysCFG(SpaceRenderingSystem.class, 100, false, true, false, true, true));
        //should not be a separate space and world renderer, both render entity sprites
        systems.add(new SysCFG(WorldRenderingSystem.class, 110, false, false, true, true, true));

        systems.add(new SysCFG(HUDSystem.class, 120, false, true, true, true, true));
    
        
        
        systems.add(new SysCFG(ScreenTransitionSystem.class, 170, true, true, true, true, true));
        
        systems.add(new SysCFG(ExpireSystem.class, 180, true, true, true, true, true));
        
        
        
        systems.add(new SysCFG(DebugSystem.class, 500, false, true, true, true, true));

        
        systems.add(new SysCFG(RemovalSystem.class, 1000, false, true, true, true, true));
    
    }
    
    public ArrayList<SysCFG> getSystems() {
        return systems;
    }
    
    public SysCFG getConfig(EntitySystem system) {
        return getConfig(system.getClass().getName());
    }
    
    public SysCFG getConfig(String className) {
        for (SysCFG sys : systems) {
            if (sys.getClassName() == className)
                return sys;
        }
        
        Gdx.app.error(this.getClass().getSimpleName(), "Could not find config for: " + className);
        return null;
    }
}


