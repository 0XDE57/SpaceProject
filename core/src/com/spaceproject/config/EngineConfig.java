package com.spaceproject.config;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class EngineConfig extends Config {
    
    public boolean vsync;
    
    public float renderScale;
    public float viewportWidth;
    public float viewportHeight;
    
    public float sprite2DScale;
    public float sprite3DScale;
    public float bodyScale;
    public float meterPerUnit;
    public int pixelPerUnit;
    
    public int physicsVelocityIterations;
    public int physicsPositionIterations;
    public int physicsStepPerFrame;
    
    public int maxNoiseGenThreads;
    
    public float defaultZoomCharacter;
    public float defaultZoomVehicle;
    
    
    @Override
    public void loadDefault() {
        vsync = true;
        
        renderScale = 30f;
        viewportWidth = 1280;
        viewportHeight = 720;
        
        sprite2DScale = 0.1f;
        sprite3DScale = 0.1f;
        bodyScale = 0.1f;
        meterPerUnit = 0.1f;
        pixelPerUnit = 2;
        
        physicsVelocityIterations = 6;
        physicsPositionIterations = 2;
        physicsStepPerFrame = 60;
        
        maxNoiseGenThreads = Gdx.app.getType() == Application.ApplicationType.Desktop ? 4 : 2;
        //maxNoiseGenThreads = Math.max(1, Runtime.getRuntime().availableProcessors()-2);
        
        defaultZoomCharacter = 0.5f;
        defaultZoomVehicle = 1.0f;
    }
}
