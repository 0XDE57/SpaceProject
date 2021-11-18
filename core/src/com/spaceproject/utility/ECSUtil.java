package com.spaceproject.utility;


import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AttachedToComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CamTargetComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;

import java.lang.reflect.Field;

public class ECSUtil {
    
    public static Component transferComponent(Entity fromEntity, Entity toEntity, Class<? extends Component> componentClass) {
        Component transferredComponent = fromEntity.remove(componentClass);
        if (transferredComponent == null) {
            Gdx.app.debug("ECSUtil", "Warning: " + DebugUtil.objString(fromEntity)
                    + " has no " + componentClass.getSimpleName()
                    + " to give to " + DebugUtil.objString(toEntity));
            return null;
        }
    
        toEntity.add(transferredComponent);
        Gdx.app.debug("ECSUtil", "transferComponent: " + DebugUtil.objString(transferredComponent)
                + ": " + DebugUtil.objString(fromEntity) + " -> " + DebugUtil.objString(toEntity));
        return transferredComponent;
    }
    
    public static void transferControl(Entity fromEntity, Entity toEntity) {
        ECSUtil.transferComponent(fromEntity, toEntity, CameraFocusComponent.class);
        ECSUtil.transferComponent(fromEntity, toEntity, ControlFocusComponent.class);
        ECSUtil.transferComponent(fromEntity, toEntity, AIComponent.class);
        ECSUtil.transferComponent(fromEntity, toEntity, ControllableComponent.class);
        ECSUtil.transferComponent(fromEntity, toEntity, CamTargetComponent.class);
    }
    
    public static Entity copyEntity(Entity entity) {
        Entity newEntity = new Entity();
        for (Component c : entity.getComponents()) {
            transferComponent(entity, newEntity, c.getClass());
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
    
    public static Array<Entity> getAttachedEntities(Engine engine, Entity parentEntity) {
        Array<Entity> cluster = new Array<>();
        cluster.add(parentEntity);
        
        ImmutableArray<Entity> attachedEntities = engine.getEntitiesFor(Family.all(AttachedToComponent.class).get());
        for (Entity attachedEntity : attachedEntities) {
            AttachedToComponent attachedTo = Mappers.attachedTo.get(attachedEntity);
            if (attachedTo.parentEntity == parentEntity) {
                cluster.add(attachedEntity);
            }
        }
        
        return cluster;
    }
    
    /*
    public static Array<Entity> getEntityClusterForNode(Engine engine, Entity entity) {
        AttachedToComponent attached = Mappers.attachedTo.get(entity);
        if (attached != null) {
            //todo: if given entity is attached and not the parent itself,
            // then travel recursively up the parent chain and return the entire cluster it belongs to
            // with no duplicate entries
            //cluster.addAll(getAttachedEntities(engine, attached.parentEntity));?
            //?
            //return getAttachedEntities(engine, attached.parentEntity);?
        }
    }*/
    
    public static String getECSString(Engine engine) {
        int entities = engine.getEntities().size();
        int components = 0;
        for (Entity ent : engine.getEntities()) {
            components += ent.getComponents().size();
        }
        int systems = engine.getSystems().size();
        return "E: " + entities + " C: " + components + " S: " + systems;
    }
    
    public static void printSystems(Engine eng) {
        for (EntitySystem sys : eng.getSystems()) {
            Gdx.app.debug("DebugUtil", sys + " (" + sys.priority + ")");
        }
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
    
}
