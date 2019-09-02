package com.spaceproject.utility;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.math.Vector2;

import java.lang.reflect.Field;

/**
 * TODO: Misc is a terrible class name
 */
public class Misc {
    
    public static Entity copyEntity(Entity entity) {
        Entity newEntity = new Entity();
        for (Component c : entity.getComponents()) {
            //TODO: broken...also this is stupid,
            //clone,
            //https://stackoverflow.com/questions/869033/how-do-i-copy-an-object-in-java
			/*
			Component copy = new (instanceof c)
			for (Field f : c.getClass().getFields()) {
				
			}*/
            newEntity.add(c);
        }
        return newEntity;
    }
    
    public static Entity closestEntity(Vector2 position, ImmutableArray<Entity> entities) {
        if (entities == null || entities.size() == 0)
            return null;
        
        Entity targetEntity = entities.first();
        float targetDist = position.dst(Mappers.transform.get(targetEntity).pos);
        for (Entity searchEnt : entities) {
            float dist = position.dst(Mappers.transform.get(searchEnt).pos);
            if (dist < targetDist) {
                targetDist = dist;
                targetEntity = searchEnt;
            }
        }
        
        return targetEntity;
    }
    
    
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
    
    public static void printEntities(Engine eng) {
        for (Entity entity : eng.getEntities()) {
            printEntity(entity);
        }
    }
    
    public static void printEntity(Entity entity) {
        System.out.println(entity.toString());
        for (Component c : entity.getComponents()) {
            System.out.println("\t" + c.toString());
            for (Field f : c.getClass().getFields()) {
                try {
                    System.out.println(String.format("\t\t%-14s %s", f.getName(), f.get(c)));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            
        }
    }
    
    
    public static void printSystems(Engine eng) {
        for (EntitySystem sys : eng.getSystems()) {
            System.out.println(sys + " (" + sys.priority + ")");
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
