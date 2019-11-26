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
        systems.add(new SysCFG(AISystem.class, 3, true, true, true, true, true));
        systems.add(new SysCFG(CameraSystem.class, 10, false, true, true, true, true));
        systems.add(new SysCFG(ControlSystem.class, 4, true, true, true, true, true));
        systems.add(new SysCFG(DebugSystem.class, 18, false, true, true, true, true));
        systems.add(new SysCFG(DesktopInputSystem.class, 1, true, true, true, true, false));
        systems.add(new SysCFG(ExpireSystem.class, 14, true, true, true, true, true));
        systems.add(new SysCFG(FixedPhysicsSystem.class, 5, true, true, true, true, true));
        systems.add(new SysCFG(HUDSystem.class, 13, false, true, true, true, true));
        systems.add(new SysCFG(MobileInputSystem.class, 2, true, true, true, false, true));
        systems.add(new SysCFG(OrbitSystem.class, 8, true, true, false, true, true));
        systems.add(new SysCFG(PlanetarySystemEntitySpawner.class, 20, true, true, false, true, true));
        systems.add(new SysCFG(RemovalSystem.class, 20, false, true, true, true, true));
        systems.add(new SysCFG(ScreenTransitionSystem.class, 17, true, true, true, true, true));
        systems.add(new SysCFG(SpaceLoadingSystem.class, 15, false, true, false, true, true));
        systems.add(new SysCFG(SpaceParallaxSystem.class, 16, true, true, false, true, true));
        systems.add(new SysCFG(SpaceRenderingSystem.class, 11, false, true, false, true, true));
        systems.add(new SysCFG(WorldLoadingSystem.class, 15, true, false, true, true, true));
        systems.add(new SysCFG(WorldRenderingSystem.class, 12, false, false, true, true, true));
        systems.add(new SysCFG(WorldWrapSystem.class, 9, true, false, true, true, true));
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


