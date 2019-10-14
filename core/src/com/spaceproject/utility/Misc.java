package com.spaceproject.utility;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.lang.reflect.Field;

/**
 * TODO: Misc is a terrible class name
 */
public class Misc {
    
    
    public static String objString(Object o) {
        if (o == null) {
            return "null";
        }
        
        //a shorter version to getSimpleName() and hashcode
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(o.hashCode());
    }
    
    public static String vecString(Vector2 vec, int decimal) {
        return MyMath.round(vec.x, decimal) + ", " + MyMath.round(vec.y, decimal);
    }
    
    public static String vecString(Vector3 vec, int decimal) {
        return MyMath.round(vec.x, decimal) + ", " + MyMath.round(vec.y, decimal) + ", " + MyMath.round(vec.z, decimal);
    }
    
    public static void printObjectFields(Object o) {
        if (o == null) {
            System.out.println("OBJECT IS NULL");
            return;
        }
        
        System.out.println(o.getClass());
        for (Field f : o.getClass().getFields()) {
            try {
                System.out.println(String.format("\t%-14s %s", f.getName(), f.get(o)));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    /**
     * https://stackoverflow.com/a/21701635
     */
    public static String formatDuration(final long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);
        
        StringBuilder b = new StringBuilder();
        b.append(hours == 0 ? "00" : hours < 10 ? "0" + hours : hours);
        b.append(":");
        b.append(minutes == 0 ? "00" : minutes < 10 ? "0" + minutes : minutes);
        b.append(":");
        b.append(seconds == 0 ? "00" : seconds < 10 ? "0" + seconds : seconds);
        return b.toString();
    }
    
    
    public static void printSystemProperties() {
        System.getProperties().list(System.out);
    }
    
    
    public static void printDisplayModes() {
        System.out.println(String.format("%s %s", Gdx.graphics.getPpiX(), Gdx.graphics.getPpiY()));
        for (Graphics.DisplayMode mode : Gdx.graphics.getDisplayModes()) {
            System.out.println(String.format("%s %s %s %s", mode.width, mode.height, mode.bitsPerPixel, mode.refreshRate));
        }
        System.out.println("-------------------------\n");
    }
    
    
}
