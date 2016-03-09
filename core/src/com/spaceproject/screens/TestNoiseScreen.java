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
	
	Texture noise;
	double scale = 40;
	long seed;
	int size = 256;
	//float off = 0;
	float zed = 0;
	static double animationSpeed = 48.0;
	float zoomScale = 40.0f;
	
	float xX = 0;
	float yY = 0;
	
	public TestNoiseScreen(SpaceProject space) {
		seed = MathUtils.random(Long.MAX_VALUE);
		noise = generateWrappingNoise4D(seed, size, scale); //generateNoise3(seed, size, scale, zed);
		
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
				   size, size,
				   zoomScale, zoomScale,
				   0, 
				   0, 0, size, size, false, false);
		
		font.draw(batch, "Seed: " + seed, 15, Gdx.graphics.getHeight() - 15);
		font.draw(batch, "Scale: " + scale, 15, Gdx.graphics.getHeight() - 30);
		font.draw(batch, "Zoom: " + zoomScale, 15, Gdx.graphics.getHeight() - 45);
		
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
			++scale;
			change = true;		
		}
		if (Gdx.input.isKeyPressed(Keys.MINUS)) {
			--scale;
			change = true;
		}
		if (Gdx.input.isKeyPressed(Keys.LEFT_BRACKET)) {
			zoomScale -= 0.5;
			change = true;
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT_BRACKET)) {
			zoomScale += 0.5;
			change = true;
		}
		
		if (change)
			noise = generateWrappingNoise4D(seed, size, scale); //noise = generateNoise3(seed, size, scale, zed);
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
	
	public static Texture generateWrappingNoise4D(long seed, int size, double scale) {
		OpenSimplexNoise noise = new OpenSimplexNoise(seed);
		
		Pixmap pixmap = new Pixmap(size, size, Format.RGB888);
	
		
		//add layer of noise
		for (int y = 0; y < pixmap.getHeight(); ++y) {
			for (int x = 0; x < pixmap.getWidth(); ++x) {
				
				//sinX, cosX. wrap X axis
				double sx = MathUtils.sin(x * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale;
				double cx = MathUtils.cos(x * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale;
				//sinY, cosY. wrap Y axis
				double sy = MathUtils.sin(y * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale;
				double cy = MathUtils.cos(y * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale;
				
				
				double i = noise.eval(sx, cx, sy, cy); //eval 4D noise using wrapped x and y axis
				i = (i * 0.5) + 0.5; // convert from range [-1:1] to [0:1]
				
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
				*/
				/*
				if (i > 0.5f){
					pixmap.setColor(new Color(1, 1, 0, (float)i));
				} else {
					pixmap.setColor(new Color(1, 0, 0, (float)(1-i)));
				}*/
				
				pixmap.setColor(new Color((float) i, (float) i , (float) i, 1));
				//if (i > 0.45f && i < 0.5f)
					//pixmap.setColor(new Color((float) 1, (float) 1 , (float) 0, 1));
				
				
				pixmap.drawPixel(x, y);

			}
		}
		

		Texture t = new Texture(pixmap);
		pixmap.dispose();
		return t;
	}
	
	public static Texture generateNoise3(long seed, int size, double scale, float z) {
		OpenSimplexNoise noise = new OpenSimplexNoise(seed);
		
		Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
		
		
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
