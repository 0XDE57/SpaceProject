package com.spaceproject.utility;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;

public class ResourceDisposer /*implements EntityListener */ {
	/*
	public ResourceDisposer(Engine engine) {
		Family family = Family.one(TextureComponent.class, Sprite3DComponent.class).get();
		engine.addEntityListener(family, this);
	}
	
	@Override
	public void entityAdded(Entity entity) {}
	
	@Override
	public void entityRemoved(Entity entity) {
		//I like this idea of auto-disposal in theory but its causing more issues than it's solving at the moment
		//namely; disposing things that I actually want to keep.
		//see: org.lwjgl.opengl.OpenGLException: Cannot use offsets when Array Buffer Object is disabled
		//potential solution: add marker component DontDisposeMePleaseComponent
		//either way, responsibility is on the caller, so just explicitly call dispose
		TextureComponent tex = Mappers.texture.get(entity);
		if (tex != null) {
			tex.texture.dispose();
			System.out.println("texture released");
		}
		
		Sprite3DComponent s3d = Mappers.sprite3D.get(entity);
		if (s3d != null) {
			s3d.renderable.dispose();
			System.out.println("renderable released");
		}
	}
	*/
	
	public static void dispose(Entity entity) {
		TextureComponent tex = Mappers.texture.get(entity);
		if (tex != null) {
			tex.texture.dispose();
			//System.out.println("texture released");
		}
		
		Sprite3DComponent s3d = Mappers.sprite3D.get(entity);
		if (s3d != null) {
			s3d.renderable.dispose();
			//System.out.println("renderable released");
		}
	}
	
	public static void disposeAll(ImmutableArray<Entity> entities, Entity ignoreEntity) {
		for (Entity entity : entities) {
			if (ignoreEntity != null && !ignoreEntity.equals(entity))
				dispose(entity);
		}
	}
}
