package com.spaceproject.config;

public class DebugConfig extends Config {
    
    public boolean spawnAsteroid;
    public boolean drawDebugUI;
    public boolean drawFPS;
    public boolean drawDiagnosticInfo;
    public boolean drawComponentList;
    public boolean drawPos;
    public boolean box2DDebugRender;
    public boolean drawBodies;
    public boolean drawJoints;
    public boolean drawAABBs;
    public boolean drawInactiveBodies;
    public boolean drawVelocities;
    public boolean drawContacts;
    public boolean drawOrbitPath;
    public boolean drawMousePos;
    public boolean drawEntityList;
    public boolean lerpCam;
    public boolean infiniteFire;

    @Override
    public void loadDefault() {
        spawnAsteroid = false;
        drawDebugUI = true;
        drawFPS = true;
        drawDiagnosticInfo = false;
        drawComponentList = false;
        drawPos = false;
        box2DDebugRender = false;
        drawBodies = true;
        drawJoints = true;
        drawAABBs = false;
        drawInactiveBodies = true;
        drawVelocities = true;
        drawContacts = true;
        drawOrbitPath = false;
        drawMousePos = false;
        drawEntityList = false;
        lerpCam = false;
        infiniteFire = false;
    }
    
}
