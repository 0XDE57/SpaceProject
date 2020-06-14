package com.spaceproject.config;


import com.badlogic.gdx.Gdx;
import com.spaceproject.systems.*;

import java.util.ArrayList;

public class SystemsConfig extends Config {
    
    ArrayList<SysCFG> systems;
    
    @Override
    public void loadDefault() {
        systems = new ArrayList<>();
        //SysCFG(entitySystem, priority, haltOnGamePause, loadInSpace, loadInWorld, loadOnDesktop, loadOnMobile)
        systems.add(new SysCFG(DesktopInputSystem.class, 10, true, true, true, true, false));
        systems.add(new SysCFG(ControllerInputSystem.class, 15, true, true, true, true, true));
        systems.add(new SysCFG(MobileInputSystem.class, 20, true, true, true, false, true));
        systems.add(new SysCFG(AISystem.class, 30, true, true, true, true, true));
        systems.add(new SysCFG(CharacterControlSystem.class, 40, true, true, true, true, true));
        systems.add(new SysCFG(ShipControlSystem.class, 50, true, true, true, true, true));
        systems.add(new SysCFG(HyperDriveSystem.class, 55, true, true, true, true, true));
        systems.add(new SysCFG(FixedPhysicsSystem.class, 60, true, true, true, true, true));
        systems.add(new SysCFG(OrbitSystem.class, 70, true, true, false, true, true));
        systems.add(new SysCFG(WorldWrapSystem.class, 80, true, false, true, true, true));
        systems.add(new SysCFG(CameraSystem.class, 90, false, true, true, true, true));
        systems.add(new SysCFG(SpaceRenderingSystem.class, 100, false, true, false, true, true));
        systems.add(new SysCFG(WorldRenderingSystem.class, 110, false, false, true, true, true));
        systems.add(new SysCFG(HUDSystem.class, 120, false, true, true, true, true));
        systems.add(new SysCFG(ExpireSystem.class, 130, true, true, true, true, true));
        systems.add(new SysCFG(WorldLoadingSystem.class, 140, true, false, true, true, true));
        systems.add(new SysCFG(SpaceLoadingSystem.class, 150, false, true, false, true, true));
        systems.add(new SysCFG(SpaceParallaxSystem.class, 160, true, true, false, true, true));
        systems.add(new SysCFG(ScreenTransitionSystem.class, 170, true, true, true, true, true));
        systems.add(new SysCFG(DebugSystem.class, 180, false, true, true, true, true));
        //systems.add(new SysCFG(PlanetarySystemEntitySpawner.class, 20, true, true, false, true, true));
        systems.add(new SysCFG(RemovalSystem.class, 200, false, true, true, true, true));
    
    }
    
    public ArrayList<SysCFG> getSystems() {
        return systems;
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


