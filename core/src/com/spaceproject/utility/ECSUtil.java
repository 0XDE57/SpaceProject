package com.spaceproject.utility;


import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AttachedToComponent;

public class ECSUtil {
    
    public static Component transferComponent(Entity fromEntity, Entity toEntity, Class<? extends Component> componentClass) {
        if (fromEntity.getComponent(componentClass) == null) {
            Gdx.app.debug("ECSUtil", "Warning: " + Misc.objString(fromEntity)
                    + " has no " + componentClass.getSimpleName()
                    + " to give to " + Misc.objString(toEntity));
            return null;
        }
    
        Component transferred = toEntity.addAndReturn(fromEntity.remove(componentClass));
        Gdx.app.debug("ECSUtil", "transferComponent: " + Misc.objString(transferred)
                + ": " + Misc.objString(fromEntity) + " -> " + Misc.objString(toEntity));
        return transferred;
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
    
}
