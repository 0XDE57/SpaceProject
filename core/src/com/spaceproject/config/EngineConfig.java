package com.spaceproject.config;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class EngineConfig extends Config {
    
    public boolean vsync;
    public float renderScale;
    public float viewportWidth;
    public float viewportHeight;
    public int physicsVelocityIterations;
    public int physicsPositionIterations;
    public int physicsStepPerFrame;
    public int maxNoiseGenThreads;
    
    @Override
    public void loadDefault() {
        vsync = true;
        renderScale = 30f;
        viewportWidth = 1280;
        viewportHeight = 720;
        physicsVelocityIterations = 6;
        physicsPositionIterations = 2;
        physicsStepPerFrame = 60;
        maxNoiseGenThreads = Gdx.app.getType() == Application.ApplicationType.Desktop ? 4 : 2; //Runtime.getRuntime().availableProcessors()-1;
    }
}
