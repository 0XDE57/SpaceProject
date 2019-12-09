package com.spaceproject.utility;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.SysCFG;
import com.spaceproject.config.SystemsConfig;
import com.spaceproject.screens.GameScreen;


public abstract class SystemLoader {
    
    
    public static void loadSystems(GameScreen game, Engine engine, boolean inSpace, SystemsConfig cfg) {
        String tag = "SystemLoader";
        Gdx.app.log(tag, inSpace ? "==========SPACE==========" : "==========WORLD==========");
        
        long time = System.currentTimeMillis();
        
        for (SysCFG sysCFG : cfg.getSystems()) {
            loadSystem(game, engine, inSpace, tag, sysCFG);
        }
        
        long now = System.currentTimeMillis();
        Gdx.app.log(tag, "systems loaded in " + (now - time) + "ms");
    }
    
    @SuppressWarnings("unchecked")
    private static void loadSystem(GameScreen game, Engine engine, boolean inSpace, String tag, SysCFG sysCFG) {
        boolean correctPlatform = (SpaceProject.isMobile() && sysCFG.isLoadOnMobile()) || (!SpaceProject.isMobile() && sysCFG.isLoadOnDesktop());
        if (!correctPlatform) {
            Gdx.app.log(tag, "Skip loading: " + sysCFG.getClassName());
            return;
        }
        
        boolean shouldBeLoaded = (inSpace && sysCFG.isLoadInSpace()) || (!inSpace && sysCFG.isLoadInWorld());
        try {
            Class<? extends EntitySystem> systemClass = (Class<? extends EntitySystem>) Class.forName(sysCFG.getClassName());
            EntitySystem systemInEngine = engine.getSystem(systemClass);
            
            boolean isLoaded = systemInEngine != null;
            if (shouldBeLoaded) {
                if (!isLoaded) {
                    EntitySystem systemToLoad = systemClass.newInstance();
                    systemToLoad.priority = sysCFG.getPriority();
                    
                    if (systemToLoad instanceof IRequireGameContext) {
                        ((IRequireGameContext) systemToLoad).initContext(game);
                    }
                    
                    if (systemToLoad instanceof EntityListener) {
                        engine.addEntityListener((EntityListener) systemToLoad);
                    }
                    
                    engine.addSystem(systemToLoad);
                    Gdx.app.log(tag, "Loaded: " + systemToLoad.getClass().getName());
                }
            } else {
                if (isLoaded) {
                    if (systemInEngine instanceof EntityListener) {
                        //listener must be removed, otherwise a reference is kept in engine (i think?)
                        //when system is re-added / re-removed down the line, the families/listeners are broken
                        engine.removeEntityListener((EntityListener) systemInEngine);
                    }
                    engine.removeSystem(systemInEngine);
                    Gdx.app.log(tag, "Unloaded: " + systemInEngine.getClass().getName());
                }
            }
        } catch (ClassNotFoundException e) {
            Gdx.app.error(tag, "Could not find " + sysCFG.getClassName(), e);
        } catch (InstantiationException e) {
            Gdx.app.error(tag, "Could not instantiate " + sysCFG.getClassName(), e);
        } catch (Exception e) {
            Gdx.app.error(tag, "Could not load " + sysCFG.getClassName(), e);
        }
    }
    
    private void loadMods(){
        //todo: allow custom loading from file, eg: http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
        //load from directory
        //eg: users can simply drop mod in location
        //  assets/mods/
        //      mymodA: a mod pack. basically same structure as assets folder with user replaceable values
        //          config: custom config settings, including SystemsConfig which will define priorities and
        //          systems: replace and add systems logic
        //          sound: replace sounds packs
        
        //Mod manager UI
        //a file browser UI to select mod, mod will be copied to mods folder
        //mod browser will list mods
        //users will be able to select mods they wish to activate
        //mod application will be granular
        //eg: a user may only want the shader from one pack, or the systems from another
        //
        
        //todo: consider security ramifications of loading arbitrary code. users need to be cautions of plugins found online
        //eg: malicious plugin, starts uploading all your files to a remote server
        //https://blog.jayway.com/2014/06/13/sandboxing-plugins-in-java/
        //https://docstore.mik.ua/orelly/java-ent/security/ch06_03.htm
    }
}
