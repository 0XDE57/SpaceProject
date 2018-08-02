package com.spaceproject.utility;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector3;

import java.lang.reflect.Field;

/** TODO: Misc is a terrible class name */
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
	
	public static Entity closestEntity(Vector3 position, ImmutableArray<Entity> entities) {
		if (entities == null || entities.size() == 0)
			return null;
		
		Entity targetEntity = entities.first();
		float targetDist = MyMath.distance(position, Mappers.transform.get(targetEntity).pos);
		for (Entity searchEnt : entities) {
			float dist = MyMath.distance(Mappers.transform.get(searchEnt).pos, position);
			if (dist < targetDist) {
				targetDist = dist;
				targetEntity = searchEnt;
			}
		}

		return targetEntity;
	}


	public static String myToString(Object e) {
		//a shorter version to getSimpleName() and hashcode
		return e.getClass().getSimpleName() + "@" + Integer.toHexString(e.hashCode());
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


}
