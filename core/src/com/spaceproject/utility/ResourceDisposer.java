package com.spaceproject.utility;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;

public class ResourceDisposer implements EntityListener {
	
	public ResourceDisposer(Engine engine) {
		Family family = Family.one(TextureComponent.class, Sprite3DComponent.class).get();
		engine.addEntityListener(family, this);
	}
	
	@Override
	public void entityAdded(Entity entity) {}
	
	@Override
	public void entityRemoved(Entity entity) {
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
}
