package com.spaceproject.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.spaceproject.SpaceProject;

public class DesktopLauncher {
    
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        //todo: test MSAA
        //config.setBackBufferConfig(8,8,8,8,16,0,8);//default
        //config.setBackBufferConfig(0, 0, 0, 0, 0, 0, 0);

        config.setWindowedMode(1280, 800);
        config.useVsync(true);
        config.setForegroundFPS(0);//disable limit for when vsync off
        config.disableAudio(true); //disable libGDX audio in favor of TuningFork

        new Lwjgl3Application(new SpaceProject(arg), config);
    }
    
}
