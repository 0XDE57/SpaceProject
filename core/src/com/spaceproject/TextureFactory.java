package com.spaceproject;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

public class TextureFactory {

	static Pixmap pixmap;
	
	public static Texture generateSpaceBackground(int tileX, int tileY, int tileSize) {
		/* Note: A Global seed of 0 causes non-unique seeds. 
		 * Consider either disallowing a seed of 0 or change formula for getting each tiles seed
		 * This probably affects other parts of the program.
		 */
		
		MathUtils.random.setSeed((long)(tileX + tileY * SpaceProject.SEED));
		//System.out.println("Tile Seed: " + (long)(tileX + tileY * SpaceProject.SEED));
		
		pixmap = new Pixmap(tileSize, tileSize, Format.RGB565);
		//pixmap = new Pixmap(tileSize, tileSize, Format.RGBA4444);
		
		int numStars = 400;
		pixmap.setColor(Color.WHITE);
		for (int i = 0; i < numStars; ++i){					
			
			int newX = MathUtils.random(tileSize);
			int newY = MathUtils.random(tileSize);
			
			pixmap.drawPixel(newX, newY);
			//pixmap.drawPixel(newX, newY, Color.rgba4444(MathUtils.random(1), MathUtils.random(1), MathUtils.random(1), 1));
		}
		
		/*
		//DEBUG - fill tile to visualize boundaries
		pixmap.setColor(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f);
		pixmap.fill();
		*/
		
		//create texture and dispose pixmap to prevent memory leak
		Texture t = new Texture(pixmap);
		pixmap.dispose(); 
		return t;
	}
}
