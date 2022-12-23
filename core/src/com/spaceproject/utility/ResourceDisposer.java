package com.spaceproject.utility;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;

public class ResourceDisposer {
    
    private static int disposedTextures, disposedS3D, destroyedBody, disposedParticle;
    private static int totalTextures, totalS3D, totalBody, totalParticle;
    private static int totalTotal;
    private static StringBuilder info = new StringBuilder();
    
    public static void dispose(Entity entity) {
        TextureComponent tex = Mappers.texture.get(entity);
        if (tex != null) {
            tex.texture.dispose();
            disposedTextures++;
        }
        
        Sprite3DComponent s3d = Mappers.sprite3D.get(entity);
        if (s3d != null) {
            s3d.renderable.dispose();
            disposedS3D++;
        }
    
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            physics.body.getWorld().destroyBody(physics.body);
            destroyedBody++;
        }
    
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null) {
            particle.pooledEffect.dispose();
            disposedParticle++;
        }
    }
    
    public static void disposeAllExcept(ImmutableArray<Entity> entities, Array<Entity> ignoreEntities) {
        for (Entity entity : entities) {
            if (ignoreEntities != null && ignoreEntities.contains(entity, false)) {
                Gdx.app.debug("ResourceDisposer", "Did NOT dispose: " + DebugUtil.objString(entity));
                continue;
            }
            
            dispose(entity);
        }
    }
    
    public static void disposeAll(ImmutableArray<Entity> entities) {
        disposeAllExcept(entities, null);
    }
    
    public static void reset() {
        //total
        totalTextures += disposedTextures;
        totalS3D += disposedS3D;
        totalParticle += disposedParticle;
        totalBody += destroyedBody;
        totalTotal = totalTextures + totalS3D + totalParticle + totalBody;
        
        //reset per frame data: should be called at very end of frame
        disposedTextures = 0;
        disposedS3D = 0;
        disposedParticle = 0;
        destroyedBody = 0;
    }
    
    public static String getTotalDisposeCount() {
        info.setLength(0);
        info.append("\n     [Texture]:  " + totalTextures);
        info.append("\n     [Sprite3D]: " + totalS3D);
        info.append("\n     [Particle]: " + totalParticle);
        info.append("\n     [B2D Body]: " + totalBody);
        info.append("\n     [Total]:    " + totalTotal); // total ;)
        reset();
        return info.toString();
    }
    
}
