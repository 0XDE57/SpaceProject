package com.spaceproject.config;


import com.badlogic.gdx.Gdx;

import java.util.ArrayList;

public class SystemsConfig extends Config {
    
    ArrayList<SysCFG> systems;
    
    @Override
    public void loadDefault() {
        systems = new ArrayList<SysCFG>();
        systems.add(new SysCFG("com.spaceproject.systems.AISystem",
                3, true, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.BoundsSystem",
                6, true, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.CameraSystem",
                10, false, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.CollisionSystem",
                7, true, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.ControlSystem",
                4, true, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.DebugUISystem",
                18, false, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.DesktopInputSystem",
                1, true, true, true, true, false));
        systems.add(new SysCFG("com.spaceproject.systems.ExpireSystem",
                14, true, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.HUDSystem",
                13, false, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.MobileInputSystem",
                2, true, true, true, false, true));
        systems.add(new SysCFG("com.spaceproject.systems.MovementSystem",
                5, true, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.OrbitSystem",
                8, true, true, false, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.ScreenTransitionSystem",
                17, true, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.SpaceLoadingSystem",
                15, false, true, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.SpaceParallaxSystem",
                16, true, true, false, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.SpaceRenderingSystem",
                11, false, true, false, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.WorldRenderingSystem",
                12, false, false, true, true, true));
        systems.add(new SysCFG("com.spaceproject.systems.WorldWrapSystem",
                9, true, false, true, true, true));
    }
    
    public ArrayList<SysCFG> getSystems() {
        return systems;
    }
    
    public SysCFG getConfig(String className) {
        for (SysCFG sys : systems) {
            if (sys.getClassName() == className)
                return sys;
        }
        
        Gdx.app.log(this.getClass().getSimpleName(), "Could not find config for: " + className);
        return null;
    }
}


