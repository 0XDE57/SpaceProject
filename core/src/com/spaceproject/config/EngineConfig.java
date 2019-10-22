package com.spaceproject.config;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class EngineConfig extends Config {
    
    public boolean vsync;
    public float renderScale;
    public float viewportWidth;
    public float viewportHeight;
    public float entityScale;
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
        entityScale = 4.0f;
        meterPerUnit = 0.1f;
        pixelPerUnit = 2;
        physicsVelocityIterations = 6;
        physicsPositionIterations = 2;
        physicsStepPerFrame = 60;
        maxNoiseGenThreads = Gdx.app.getType() == Application.ApplicationType.Desktop ? 4 : 2; //Runtime.getRuntime().availableProcessors()-1;
        
        defaultZoomCharacter = 0.5f;
        defaultZoomVehicle = 1.0f;
    }
}
