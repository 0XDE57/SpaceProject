package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.utility.OpenSimplexNoise;

public class TestNoiseScreen extends ScreenAdapter {
	
	SpriteBatch batch = new SpriteBatch();
	private BitmapFont font;
	
	int mapSize = 256;	
	float pixelSize = 3.0f;
	
	long seed;
	Texture noise;
	double scale = 40;
	int octaves = 4;
	float persistence = 0.5f;
	float lacunarity = 2;
	
	
	float xX = 0;
	float yY = 0;
	
	public TestNoiseScreen(SpaceProject space) {
		seed = MathUtils.random(Long.MAX_VALUE);
		noise = generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity);
		
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
		
		font.setColor(1, 1, 1, 1);
	}

	public void render(float delta) {
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		batch.begin();
		
		//batch.draw(noise, Gdx.graphics.getWidth()/2-noise.getWidth()/2, Gdx.graphics.getHeight()/2-noise.getHeight()/2);
		
		//batch.draw(noise, 0, 0);
		//batch.draw(noise, size, 0);
		//batch.draw(noise, 0, size);
		//batch.draw(noise, size, size);

		batch.draw(noise, xX, yY,
				   0, 0,
				   mapSize, mapSize,
				   pixelSize, pixelSize,
				   0, 
				   0, 0, mapSize, mapSize, false, false);
		
		font.draw(batch, "Seed: " + seed, 15, Gdx.graphics.getHeight() - 15);
		font.draw(batch, "Scale: " + scale, 15, Gdx.graphics.getHeight() - 30);
		font.draw(batch, "Zoom: " + pixelSize, 15, Gdx.graphics.getHeight() - 45);
		
		batch.end();
		
		
		if (Gdx.input.isKeyPressed(Keys.LEFT))
			xX++;
		if (Gdx.input.isKeyPressed(Keys.RIGHT))
			xX--;
		if (Gdx.input.isKeyPressed(Keys.UP))
			yY--;
		if (Gdx.input.isKeyPressed(Keys.DOWN))
			yY++;
		
		boolean change = false;
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)){
			seed = MathUtils.random(Long.MAX_VALUE);
			change = true;
		}			
		if (Gdx.input.isKeyPressed(Keys.EQUALS)) {
			pixelSize += 0.5;
			change = true;		
		}
		if (Gdx.input.isKeyPressed(Keys.MINUS)) {
			pixelSize -= 0.5;
			change = true;
		}
		
		if (Gdx.input.isKeyPressed(Keys.LEFT_BRACKET)) {
			++scale;
			change = true;
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT_BRACKET)) {
			--scale;
			change = true;
		}
		
		if (change)
			noise = generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity); //noise = generateNoise3(seed, size, scale, zed);
	}
	
	public static Texture generateNoise(long seed, int size, double scale) {
		OpenSimplexNoise noise = new OpenSimplexNoise(seed);
		
		Pixmap pixmap = new Pixmap(size, size, Format.RGB888);
		
		
		//add layer of noise
		for (int y = 0; y < pixmap.getHeight(); ++y) {
			for (int x = 0; x < pixmap.getWidth(); ++x) {
				
				double nx = x / scale, ny = y / scale;
				double i = noise.eval(nx, ny, 0);
				i = (i * 0.5) + 0.5; // convert from range [-1:1] to [0:1]
				
				
				pixmap.setColor(new Color((float) i, (float) i , (float) i, 1));
				pixmap.drawPixel(x, y);

			}
		}
		

		Texture t = new Texture(pixmap);
		pixmap.dispose();
		return t;
	}
	
	public static Texture generateWrappingNoise4D(long seed, int size, double scale, int octaves, float persistence, float lacunarity) {
		float[][] map = new float[size][size];
		
		OpenSimplexNoise noise = new OpenSimplexNoise(seed);
	
		float minNoise = Float.MAX_VALUE;
		float maxNoise = Float.MIN_VALUE;

		for (int x = 0; x < size; ++x) {
			for (int y = 0; y < size; ++y) {
				float amplitude = 1;
				float frequency = 1;
				float noiseHeight = 0;
				
				for (int oct = 0; oct < octaves; ++oct) {
					// sinX, cosX. wrap X axis
					double sx = (MathUtils.sin(x * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale) * frequency;
					double cx = (MathUtils.cos(x * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale) * frequency;
					// sinY, cosY. wrap Y axis
					double sy = (MathUtils.sin(y * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale) * frequency;
					double cy = (MathUtils.cos(y * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale) * frequency;

					double i = noise.eval(sx, cx, sy, cy); // eval 4D noise using wrapped x and y axis
					
					//i = (i * 0.5) + 0.5; // convert from range [-1:1] to [0:1]
					
					//map[x][y] = (float) (i * 0.5f) + 0.5f; // convert from range [-1:1] to [0:1]
					noiseHeight += (float) (i * amplitude);
					
					amplitude += persistence;
					frequency += lacunarity;
					
				}
				
				if (noiseHeight > maxNoise) maxNoise = noiseHeight;
				if (noiseHeight < minNoise) minNoise = noiseHeight;
				
				map[x][y] = noiseHeight;
			}			
		}
		System.out.println("Origin MIN/MAX: " + minNoise + "/" + maxNoise);
		
		//normalize
		float minNoiseNormal = Float.MAX_VALUE;
		float maxNoiseNormal = Float.MIN_VALUE;

		for (int x = 0; x < size; ++x) {
			for (int y = 0; y < size; ++y) {
				map[x][y] = inverseLerp(minNoise, maxNoise, map[x][y]);
				float normal = map[x][y];
				if (normal > maxNoiseNormal) maxNoiseNormal = normal;
				if (normal < minNoiseNormal) minNoiseNormal = normal;
			}
		}
		System.out.println("Normal MIN/MAX: " + minNoiseNormal + "/" + maxNoiseNormal);
		
		//create image
		Pixmap pixmap = new Pixmap(size, size, Format.RGB888);
		for (int x = 0; x < size; ++x) {
			for (int y = 0; y < size; ++y) {
				/*
				if (i > 0.85f)
					pixmap.setColor(new Color((float) 1, (float) 1, (float) 1, 1));			
				else if (i > 0.8f)
					pixmap.setColor(new Color((float) 90/255, (float) 45/255, (float) 10/255, 1));
				else if (i > 0.5f)
					pixmap.setColor(new Color((float) 0, (float) (1-i), (float) 0, 1));
				else if (i > 0.43f)
					pixmap.setColor(new Color((float) 0.9, (float) 0.7, (float) 0, 1));
				else if (i > 0.3f)
					pixmap.setColor(new Color((float) 0, (float) 0, (float) (1-i), 1));
				if (i > 0.5f){
					pixmap.setColor(new Color(1, 1, 0, (float)i));
				} else {
					pixmap.setColor(new Color(1, 0, 0, (float)(1-i)));
				}*/
				//float i = (map[xX][yY] * 0.5f) + 0.5f; // convert from range [-1:1] to [0:1];
				
				float i = map[x][y];
				pixmap.setColor(new Color(i, i , i, 1));
				
				pixmap.drawPixel(x, y);
			}
		}
		

		Texture t = new Texture(pixmap);
		pixmap.dispose();
		return t;
	}
	
	private static float inverseLerp(float minNoise, float maxNoise, float f) {
		return (f - minNoise) / (maxNoise - minNoise);
	}

	public static Texture generateNoise3(long seed, int size, double scale, float z) {
		OpenSimplexNoise noise = new OpenSimplexNoise(seed);
		
		Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
		double animationSpeed = 48.0;
		
		//add layer of noise
		for (int y = 0; y < pixmap.getHeight(); ++y) {
			for (int x = 0; x < pixmap.getWidth(); ++x) {
				
				double nx = x / scale, ny = y / scale;
				
				
				double i = noise.eval(nx, ny, z/animationSpeed); //noise.eval(nx, ny, 0);
				i = (i * 0.5) + 0.5; // convert from range [-1:1] to [0:1]
				
				if (i > 0.5f){
					pixmap.setColor(new Color(1, 1, 0, (float)i));
				} else {
					pixmap.setColor(new Color(1, 0, 0, (float)(1-i)));
				}
				
				//pixmap.setColor(new Color((float) i, (float) i , (float) i, 1));
				
				pixmap.drawPixel(x, y);

			}
		}
		

		Texture t = new Texture(pixmap);
		pixmap.dispose();
		return t;
	}
	
	public void resize(int width, int height) {
		Gdx.app.log("screen", width + ", " + height);
	}

	public void dispose() { }

	public void hide() { }

	public void pause() { }

	public void resume() { }
	
}
