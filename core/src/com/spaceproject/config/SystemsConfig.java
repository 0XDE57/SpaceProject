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
        // sorted by priority. engine executes systems in order of priority from low value to high.
        // gaps between values for easy insertion and of new systems and easy tuning/adjusting.
        // flags (handled by @SystemLoader and @GameScreen):
        // - priority: order of execution. order is important for logic and rendering.
        // - haltOnGamePause: if true, system stops processing when game is paused.
        // - loadInSpace: if true, system will be loaded when player is in space.
        // - loadInWorld: if true, system will be loaded when player is on a planet.
        // - loadOnDesktop: if true, will be loaded on desktop platform.
        // - loadOnMobile: if true, will be loaded on iOS or Android platform.
        
        systems = new ArrayList<>();
        systems.add(new SysCFG(ClearScreenSystem.class, 0, false, true, true, true, true));
        
        //---input---
        systems.add(new SysCFG(DesktopInputSystem.class, 10, true, true, true, true, false));
        systems.add(new SysCFG(ControllerInputSystem.class, 15, true, true, true, true, true));
        systems.add(new SysCFG(MobileInputSystem.class, 20, true, true, true, false, true));
        
        
        //---logic---
        systems.add(new SysCFG(AISystem.class, 30, true, true, true, true, true));
        
        systems.add(new SysCFG(CharacterControlSystem.class, 40, true, true, true, true, true));
        systems.add(new SysCFG(DashSystem.class, 41, true, true, true, true, true));
        
        systems.add(new SysCFG(ShipControlSystem.class, 50, true, true, true, true, true));
        systems.add(new SysCFG(BarrelRollSystem.class, 52, true, true, true, true, true));
        systems.add(new SysCFG(ShieldSystem.class, 53, true, true, true, true, true));
        systems.add(new SysCFG(CannonSystem.class, 55, true, true, true, true, true));
        systems.add(new SysCFG(ChargeCannonSystem.class, 56, true, true, true, true, true));
        systems.add(new SysCFG(HyperDriveSystem.class, 59, true, true, true, true, true));
        
        systems.add(new SysCFG(OrbitSystem.class, 60, true, true, false, true, true));
        systems.add(new SysCFG(WorldWrapSystem.class, 65, true, false, true, true, true));
        
        systems.add(new SysCFG(FixedPhysicsSystem.class, 70, true, true, true, true, true));
        
    
    
        //----render----
        //todo: rendering pipeline
        // - screen clear
        // - cam update
        // - asteroid renderer -> shape render pre sprite
        // - sprite 2D renderer
        // - sprite 3D renderer
        // - shield renderer -> shape render post sprite (possible shader for glow? box2d-lights?)
        // - particle renderer -> post sprite
        //could have multilayer rendering if needed in rendering pipeline: particle layer pre sprite (under sprites), particle layer post sprite (over sprites)
        systems.add(new SysCFG(CameraSystem.class, 80, false, true, true, true, true));
        systems.add(new SysCFG(SpaceParallaxSystem.class, 85, true, true, false, true, true));
        systems.add(new SysCFG(WorldRenderingSystem.class, 86, false, false, true, true, true));
        systems.add(new SysCFG(Sprite2DRenderSystem.class, 90, false, true, true, true, true));
        systems.add(new SysCFG(Sprite3DRenderSystem.class, 91, false, true, true, true, true));
        systems.add(new SysCFG(ShieldRenderSystem.class, 100, false, true, true, true, true));
        systems.add(new SysCFG(ParticleSystem.class, 110, true, true, true, true, true));
        systems.add(new SysCFG(HUDSystem.class, 200, false, true, true, true, true));
        
        systems.add(new SysCFG(ScreenTransitionSystem.class, 300, true, true, true, true, true));
        //---loading---
        systems.add(new SysCFG(WorldLoadingSystem.class, 301, true, false, true, true, true));
        systems.add(new SysCFG(SpaceLoadingSystem.class, 302, false, true, false, true, true));
        //systems.add(new SysCFG(SpaceRespawnSystem.class, ??, true, true, false, true, true));
        //systems.add(new SysCFG(PlanetarySystemEntitySpawner.class, ??, true, true, false, true, true));
    
    
    
        systems.add(new SysCFG(ExpireSystem.class, 500, true, true, true, true, true));
        
        systems.add(new SysCFG(DebugSystem.class, 900, false, true, true, true, true));
        
        
        //Should always be last system fired. This is where entities flagged with the removal component are removed from engine
        //and resources are auto-disposed. Any systems processed after this should be careful if they rely
        //on disposable data (eg: Textures) once an entity has been removed from the engine.
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
            if (sys.getClassName().equals(className))
                return sys;
        }
        
        Gdx.app.error(this.getClass().getSimpleName(), "Could not find config for: " + className);
        return null;
    }
}


