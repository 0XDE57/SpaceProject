package com.spaceproject;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.ConfigManager;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.config.SystemsConfig;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;

public class SpaceProject extends Game {
    
    public static final String TITLE = "a space project";
    public static final String VERSION = "pre-alpha";//<α
    
    public static long SEED = 4; //test seed
    
    private static boolean isMobile;
    
    public static ConfigManager configManager;
    public static SystemsConfig systemsCFG;
    public static EntityConfig entityCFG;
    public static CelestialConfig celestCFG;
    public static WorldConfig worldCFG;
    //public static UIConfig uiCFG;
    public static KeyConfig keyCFG;
    //public static MiniMapConfig miniMapCFG;
    //public static DebugConfig debugCFG;
    
    
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
        
        systemsCFG = new SystemsConfig();
        systemsCFG.loadDefault();
        
        entityCFG = new EntityConfig();
        entityCFG.loadDefault();
        
        keyCFG = new KeyConfig();
        keyCFG.loadDefault();
        
        celestCFG = new CelestialConfig();
        celestCFG.loadDefault();
        
        worldCFG = new WorldConfig();
        worldCFG.loadDefault();
        
        //uiCFG = new UIConfig();
        //uiCFG.loadDefault();
        
        //miniMapCFG = new MiniMapConfig();
        //miniMapCFG.loadDefault();
        
        //debugCFG = new DebugConfig();
        //debugCFG.loadDefault();
    }
    
    
    public static boolean isMobile() {
        return isMobile;
    }
}
