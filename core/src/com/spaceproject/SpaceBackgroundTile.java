package com.spaceproject;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.systems.RenderingSystem;

public class SpaceBackgroundTile {
	public final float x;
	public final float y;
	public final float depth;
	public final Texture tex;
	
	public SpaceBackgroundTile(int tileX, int tileY, float renderDepth) {
		
		int tileSize = 3200;
		
		x = tileX * tileSize;
		y = tileY * tileSize;
		
		//depth = renderDepth;
		depth = 0;
		Vector2 tile = RenderingSystem.getTilePos(tileX, tileY);
		tex = TextureFactory.generateSpaceBackground((int)tile.x, (int)tile.x, tileSize);
	}
	
}
