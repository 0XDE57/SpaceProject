package com.spaceproject.utility;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;

public class ResourceDisposer {
    
    public static void dispose(Entity entity) {
        TextureComponent tex = Mappers.texture.get(entity);
        if (tex != null) {
            tex.texture.dispose();
            Gdx.app.debug("ResourceDisposer", "texture released: " + Misc.objString(entity));
        }
        
        Sprite3DComponent s3d = Mappers.sprite3D.get(entity);
        if (s3d != null) {
            s3d.renderable.dispose();
            Gdx.app.debug("ResourceDisposer", "renderable released: " + Misc.objString(entity));
        }
    
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            physics.body.getWorld().destroyBody(physics.body);
            Gdx.app.debug("ResourceDisposer", "body destroyed: " + Misc.objString(entity));
        }
    
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null) {
            particle.pooledEffect.dispose();
            Gdx.app.debug("ResourceDisposer", "particle released: " + Misc.objString(entity));
        }
    }
    
    public static void disposeAllExcept(ImmutableArray<Entity> entities, ImmutableArray<Entity> ignoreEntities) {
        for (Entity entity : entities) {
            if (ignoreEntities != null && ignoreEntities.contains(entity, false)) {
                Gdx.app.debug("ResourceDisposer", "Did not dispose: " + Misc.objString(entity));
                
                continue;
            }
            
            dispose(entity);
        }
    }
    
    public static void disposeAll(ImmutableArray<Entity> entities) {
        disposeAllExcept(entities, null);
    }
    
}
