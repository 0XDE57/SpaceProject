package com.spaceproject.utility;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.SysCFG;
import com.spaceproject.config.SystemsConfig;
import com.spaceproject.screens.GameScreen;


public abstract class SystemLoader {
    
    private static String logSource = "SystemLoader";
    
    public static void loadSystems(GameScreen game, Engine engine, boolean inSpace, SystemsConfig cfg) {
        Gdx.app.log(logSource, inSpace ? "==========SPACE==========" : "==========WORLD==========");
        
        long time = System.currentTimeMillis();
        
        for (SysCFG sysCFG : cfg.getSystems()) {
            loadUnloadSystems(game, engine, inSpace, sysCFG);
        }
        
        long now = System.currentTimeMillis();
        Gdx.app.log(logSource, "systems loaded in " + (now - time) + "ms");
    }
    
    @SuppressWarnings("unchecked")
    private static void loadUnloadSystems(GameScreen game, Engine engine, boolean inSpace, SysCFG sysCFG) {
        boolean correctPlatform = (SpaceProject.isMobile() && sysCFG.isLoadOnMobile()) || (!SpaceProject.isMobile() && sysCFG.isLoadOnDesktop());
        if (!correctPlatform) {
            Gdx.app.log(logSource, "Skip:   " +  String.format("%-4d ", sysCFG.getPriority()) + sysCFG.getClassName());
            return;
        }
        
        boolean shouldBeLoaded = (inSpace && sysCFG.isLoadInSpace()) || (!inSpace && sysCFG.isLoadInWorld());
        try {
            Class<? extends EntitySystem> systemClass = (Class<? extends EntitySystem>) Class.forName(sysCFG.getClassName());
            EntitySystem systemInEngine = engine.getSystem(systemClass);
            
            boolean isLoaded = systemInEngine != null;
            if (shouldBeLoaded) {
                if (!isLoaded) {
                    load(game, engine, sysCFG.getPriority(), systemClass);
                }
            } else {
                if (isLoaded) {
                    unLoad(engine, systemInEngine);
                }
            }
        } catch (ClassNotFoundException e) {
            Gdx.app.error(logSource, "Could not find " + sysCFG.getClassName(), e);
        } catch (InstantiationException e) {
            Gdx.app.error(logSource, "Could not instantiate " + sysCFG.getClassName(), e);
        } catch (Exception e) {
            Gdx.app.error(logSource, "Could not load " + sysCFG.getClassName(), e);
        }
    }
    
    private static void load(GameScreen game, Engine engine, int priority, Class<? extends EntitySystem> systemClass) throws InstantiationException, IllegalAccessException {
        EntitySystem systemToLoad = systemClass.newInstance();
        systemToLoad.priority = priority;
        
        //auto-hookup interfaces
        if (systemToLoad instanceof IRequireGameContext) {
            ((IRequireGameContext) systemToLoad).initContext(game);
        }
        
        if (systemToLoad instanceof EntityListener) {
            engine.addEntityListener((EntityListener) systemToLoad);
        }
        
        engine.addSystem(systemToLoad);
        Gdx.app.log(logSource, "Loaded: " + String.format("%-4d ", systemToLoad.priority) + systemToLoad.getClass().getName());
    }
    
    private static void unLoad(Engine engine, EntitySystem systemInEngine) {
        if (systemInEngine instanceof EntityListener) {
            //listener must be removed, otherwise a reference is kept in engine (i think?)
            //when system is re-added / re-removed down the line, the families/listeners are broken
            engine.removeEntityListener((EntityListener) systemInEngine);
        }
        if (systemInEngine instanceof Disposable) {
            //clean up
            ((Disposable)systemInEngine).dispose();
        }
        
        engine.removeSystem(systemInEngine);
        Gdx.app.log(logSource, "Unload: " + String.format("%-4d ", systemInEngine.priority) + systemInEngine.getClass().getName());
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
