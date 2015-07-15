package com.spaceproject;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

public class TextureFactory {

	static Pixmap pixmap;
	
	public static Texture generateSpaceBackground(int tileX, int tileY, int tileSize) {
		MathUtils.random.setSeed((long)(tileX + tileY * SpaceProject.SEED));
		
		pixmap = new Pixmap(tileSize, tileSize, Format.RGBA8888);		
		
		int numStars = 3000;
		pixmap.setColor(Color.WHITE);
		for (int i = 0; i < numStars; ++i){					
			
			int newX = MathUtils.random(tileSize);
			int newY = MathUtils.random(tileSize);
			
			pixmap.drawPixel(newX, newY);
		}
		
		return new Texture(pixmap);
	}
}
