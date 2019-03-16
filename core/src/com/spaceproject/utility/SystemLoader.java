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
        
        for (SysCFG sysCFG : cfg.getSystems()) {
            LoadSystem(game, engine, inSpace, tag, sysCFG);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void LoadSystem(GameScreen game, Engine engine, boolean inSpace, String tag, SysCFG sysCFG) {
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
                    
                    engine.addSystem(systemToLoad);
                    Gdx.app.log(tag, "Loaded: " + systemToLoad.getClass().getName());
                }
            } else {
                if (isLoaded) {
                    if (systemInEngine instanceof EntityListener) {
                        //listener must be removed, other wise a reference is kept in engine (i think)
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
}
