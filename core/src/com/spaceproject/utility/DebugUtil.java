package com.spaceproject.utility;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;

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
    
}
