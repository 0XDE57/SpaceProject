package com.spaceproject.utility;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;

public class ResourceDisposer {
    
    static boolean logDispose = false;
    
    public static void dispose(Entity entity) {
        TextureComponent tex = Mappers.texture.get(entity);
        if (tex != null) {
            tex.texture.dispose();
            
            if (logDispose)
                Gdx.app.log("ResourceDisposer", "texture released: " + Misc.objString(entity));
        }
        
        Sprite3DComponent s3d = Mappers.sprite3D.get(entity);
        if (s3d != null) {
            s3d.renderable.dispose();
            
            if (logDispose)
                Gdx.app.log("ResourceDisposer", "renderable released: " + Misc.objString(entity));
        }
    }
    
    public static void disposeAllExcept(ImmutableArray<Entity> entities, ImmutableArray<Entity> ignoreEntities) {
        for (Entity entity : entities) {
            if (ignoreEntities != null && ignoreEntities.contains(entity, false)) {
                if (logDispose)
                    Gdx.app.log("ResourceDisposer", "Did not dispose: " + Misc.objString(entity));
                
                continue;
            }
            
            dispose(entity);
        }
    }
    
    public static void disposeAll(ImmutableArray<Entity> entities) {
        disposeAllExcept(entities, null);
    }
}
