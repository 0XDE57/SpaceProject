package com.spaceproject.utility;


import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.spaceproject.math.MyMath;

import java.lang.reflect.Field;

public class DebugUtil {
    
    public static String getMemory() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        //long javaHeap = Gdx.app.getJavaHeap();
        //long nativeHeap = Gdx.app.getNativeHeap();
        //all 3 values seem to agree
        //memory += ", java heap: " + MyMath.formatBytes(javaHeap) + ", native heap: " + MyMath.formatBytes(nativeHeap);
        return "Mem: " + MyMath.formatBytes(used);
    }
    
    public static String getECSString(Engine engine) {
        int entities = engine.getEntities().size();
        int components = 0;
        for (Entity ent : engine.getEntities()) {
            components += ent.getComponents().size();
        }
        int systems = engine.getSystems().size();
        return "E: " + entities + " C: " + components + " S: " + systems;
    }
    
    public static void printEntities(Engine eng) {
        for (Entity entity : eng.getEntities()) {
            printEntity(entity);
        }
    }
    
    public static void printEntity(Entity entity) {
        Gdx.app.debug("DebugUtil", entity.toString());
        for (Component c : entity.getComponents()) {
            Gdx.app.debug("DebugUtil", "\t" + c.toString());
            for (Field f : c.getClass().getFields()) {
                try {
                    Gdx.app.debug("DebugUtil", String.format("\t\t%-14s %s", f.getName(), f.get(c)));
                } catch (IllegalArgumentException e) {
                    Gdx.app.error("DebugUtil", "failed to read fields", e);
                } catch (IllegalAccessException e) {
                    Gdx.app.error("DebugUtil", "fail to read fields", e);
                }
            }
        }
    }
    
    public static void printSystems(Engine eng) {
        for (EntitySystem sys : eng.getSystems()) {
            Gdx.app.debug("DebugUtil", sys + " (" + sys.priority + ")");
        }
    }
    
}
