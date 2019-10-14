package com.spaceproject.config;

import com.badlogic.gdx.Input.Keys;

public class KeyConfig extends Config {
    
    //---player controls---
    public int forward;
    public int right;
    public int left;
    public int back;
    public int alter;
    public int attack;
    public int defend;
    public int changeVehicle;
    public int land;
    
    //---UI controls---
    public int toggleHUD;
    public int toggleEdgeMap;
    public int toggleSpaceMap;
    
    //---screen controls---
    public int fullscreen;
    public int vsync;
    public int zoomOut;
    public int zoomIn;
    public int resetZoom;
    public int zoomCharacter;
    public int zoomSpace;
    public int rotateLeft;
    public int rotateRight;
    
    //---debug menu controls---
    public int toggleDebug;
    public int togglePos;
    public int toggleComponents;
    public int toggleBounds;
    public int toggleFPS;
    public int toggleOrbit;
    public int toggleVector;
    public int toggleMenu;
    
    
    public void loadDefault() {
        //player
        forward = Keys.W;
        right = Keys.D;
        left = Keys.A;
        back = Keys.S;
        alter = Keys.ALT_LEFT;
        attack = Keys.SPACE;
        defend = Keys.SHIFT_LEFT;
        changeVehicle = Keys.G;
        land = Keys.T;
        
        //ui
        toggleHUD = Keys.H;
        toggleEdgeMap = Keys.M;
        toggleSpaceMap = Keys.P;
        
        //screen
        fullscreen = Keys.F11;
        vsync = Keys.F8;
        zoomOut = Keys.MINUS;
        zoomIn = Keys.EQUALS;
        resetZoom = Keys.PERIOD;
        zoomCharacter = Keys.SLASH;
        zoomSpace = Keys.COMMA;
        rotateLeft = Keys.LEFT_BRACKET;
        rotateRight = Keys.RIGHT_BRACKET;
        
        //debug menu
        toggleDebug = Keys.F3;
        togglePos = Keys.NUMPAD_0;
        toggleComponents = Keys.NUMPAD_1;
        toggleBounds = Keys.NUMPAD_2;
        toggleFPS = Keys.NUMPAD_3;
        toggleOrbit = Keys.NUMPAD_4;
        toggleVector = Keys.NUMPAD_5;
        toggleMenu = Keys.NUMPAD_9;
    }
    
}
