package com.spaceproject.config;


public class SystemPriorityConfig extends Config {
    
    public int aiSystem;
    public int boundsSystem;
    public int cameraSystem;
    public int collisionSystem;
    public int controlSystem;
    public int debugUISystem;
    public int desktopInputSystem;
    public int expireSystem;
    public int hudSystem;
    public int mobileInputSystem;
    public int movementSystem;
    public int orbitSystem;
    public int screenTransitionSystem;
    public int spaceLoadingSystem;
    public int spaceParallaxSystem;
    public int spaceRenderingSystem;
    public int worldRenderingSystem;
    public int worldWrapSystem;
    
    @Override
    public void loadDefault() {
        //input
        desktopInputSystem = 1;
        mobileInputSystem = 2;
        aiSystem = 3;
        controlSystem = 4;
        
        //movement
        movementSystem = 5;
        boundsSystem = 6;
        collisionSystem = 7;
        orbitSystem = 8;
        worldWrapSystem = 9;
        
        //rendering
        cameraSystem = 10;
        spaceRenderingSystem = 11;
        worldRenderingSystem = 12;
        hudSystem = 13;
        
        //loading
        expireSystem = 14;
        spaceLoadingSystem = 15;
        spaceParallaxSystem = 16;
        screenTransitionSystem = 17;
        
        //debug
        debugUISystem = 18;
    }
}
