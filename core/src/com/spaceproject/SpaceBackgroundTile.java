package com.spaceproject;

import com.badlogic.gdx.graphics.Texture;
import com.spaceproject.generation.TextureFactory;

public class SpaceBackgroundTile {
	public final float x;
	public final float y;
	public final float tileX;
	public final float tileY;
	public final float depth;
	public final int size;
	public final Texture tex;
	
	public SpaceBackgroundTile(int tileX, int tileY, float renderDepth, int tileSize) {
			
		this.tileX = tileX;
		this.tileY = tileY;		
		x = tileX * tileSize;
		y = tileY * tileSize;		
		size = tileSize;		
		depth = renderDepth;
		
		//generate texture
		tex = TextureFactory.generateSpaceBackground(tileX, tileY, tileSize);
	}
	
}
