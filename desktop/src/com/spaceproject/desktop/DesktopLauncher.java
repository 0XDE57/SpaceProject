package com.spaceproject.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.spaceproject.SpaceProject;

public class DesktopLauncher {
    
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    
        config.setWindowedMode(1280, 800);
        config.useVsync(true);
        config.setForegroundFPS(0);//disable limit for when vsync off
        
        new Lwjgl3Application(new SpaceProject(), config);
    }
    
}
