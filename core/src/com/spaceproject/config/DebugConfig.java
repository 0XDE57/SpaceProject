package com.spaceproject.config;

public class DebugConfig extends Config {
    
    public boolean drawDebugUI;
    public boolean drawFPS;
    public boolean drawExtraInfo;
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
    
    
    @Override
    public void loadDefault() {
        drawDebugUI = true;
        drawFPS = true;
        drawExtraInfo = true;
        drawComponentList = false;
        drawPos = false;
        box2DDebugRender = true;
        drawBodies = true;
        drawJoints = true;
        drawAABBs = true;
        drawInactiveBodies = true;
        drawVelocities = true;
        drawContacts = true;
        drawOrbitPath = false;
        drawMousePos = false;
        drawEntityList = false;
    }
    
}
