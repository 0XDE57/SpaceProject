package com.spaceproject;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.spaceproject.config.ConfigManager;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;

public class SpaceProject extends Game {
    
    public static final String TITLE = "a space project";
    public static final String VERSION = "pre-alpha";//<α
    
    private static boolean isMobile;
    
    public static ConfigManager configManager;
    
    @Override
    public void create() {
        MyScreenAdapter.game = this;
        
        isMobile = Gdx.app.getType() != Application.ApplicationType.Desktop;
        
        loadConfigs();
        
        setScreen(new TitleScreen(this));
    }
    
    
    private static void loadConfigs() {
        configManager = new ConfigManager();
        configManager.init();
    }
    
    
    public static boolean isMobile() {
        return isMobile;
    }
}
