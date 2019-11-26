package com.spaceproject.utility;


import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

public class ECSUtil {
    
    public static Component transferComponent(Entity fromEntity, Entity toEntity, Class<? extends Component> componentClass) {
        if (fromEntity.getComponent(componentClass) == null) {
            return null;
        }
    
        Component transferred = toEntity.addAndReturn(fromEntity.remove(componentClass));
        Gdx.app.log("ECSUtil", "transferComponent: " + Misc.objString(transferred)
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
    
}
