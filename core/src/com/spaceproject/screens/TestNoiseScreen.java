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
import com.spaceproject.utility.NoiseGen;
import com.spaceproject.utility.OpenSimplexNoise;

public class TestNoiseScreen extends ScreenAdapter {
	
	SpriteBatch batch = new SpriteBatch();
	private BitmapFont font;
	
	int mapSize = 128;	
	float pixelSize = 3.0f;
	
	long seed;
	Texture noise;
	double scale = 40;//30 - 100
	int octaves = 4;
	float persistence = 0.5f;//0 - 1
	float lacunarity = 2;//1 - x
	
	float xX = 0;
	float yY = 0;
	
	public TestNoiseScreen(SpaceProject space) {
		seed = MathUtils.random(Long.MAX_VALUE);
		noise = createNoiseMapTex(NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity));
		
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
		
		font.setColor(1, 1, 1, 1);
	}

	public void render(float delta) {
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		batch.begin();
		
		//batch.draw(noise, Gdx.graphics.getWidth()/2-noise.getWidth()/2, Gdx.graphics.getHeight()/2-noise.getHeight()/2);		
		//batch.draw(noise, 0, 0);
		//batch.draw(noise, mapSize, 0);
		//batch.draw(noise, 0, mapSize);
		//batch.draw(noise, mapSize, mapSize);

		batch.draw(noise, xX, yY,
				   0, 0,
				   mapSize, mapSize,
				   pixelSize, pixelSize,
				   0, 
				   0, 0, mapSize, mapSize, false, false);
		
		font.draw(batch, "Seed: " + seed, 15, Gdx.graphics.getHeight() - 15);
		font.draw(batch, "Zoom: " + pixelSize, 15, Gdx.graphics.getHeight() - 30);
		font.draw(batch, "Scale: " + scale, 15, Gdx.graphics.getHeight() - 45);
		font.draw(batch, "Octaves: " + octaves, 15, Gdx.graphics.getHeight() - 60);
		font.draw(batch, "Persistence: " + persistence, 15, Gdx.graphics.getHeight() - 75);
		font.draw(batch, "Lacunarity: " + lacunarity, 15, Gdx.graphics.getHeight() - 90);
		
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
		//TODO: make UI sliders for these values
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
			--scale;
			change = true;
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT_BRACKET)) {
			++scale;
			change = true;
		}
		
		if (Gdx.input.isKeyPressed(Keys.Q)) {
			octaves--;
			change = true;
		}
		if (Gdx.input.isKeyPressed(Keys.W)) {
			octaves++;
			change = true;
		}
		
		if (Gdx.input.isKeyPressed(Keys.A)) {
			persistence -= 0.5f;
			change = true;
		}
		if (Gdx.input.isKeyPressed(Keys.S)) {
			persistence += 0.5f;
			change = true;
		}
		
		if (Gdx.input.isKeyPressed(Keys.Z)) {
			lacunarity -= 0.2f;
			change = true;
		}
		if (Gdx.input.isKeyPressed(Keys.X)) {
			lacunarity += 0.2f;
			change = true;
		}
		
		if (change)
			noise = createNoiseMapTex(NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity)); //noise = generateNoise3(seed, size, scale, zed);
	}
	
	
	private static Texture createNoiseMapTex(float[][] map) {
		//create image
		Pixmap pixmap = new Pixmap(map.length, map.length, Format.RGB888);
		for (int x = 0; x < map.length; ++x) {
			for (int y = 0; y < map.length; ++y) {
				
				float i = map[x][y];
				pixmap.setColor(new Color(i, i , i, 1));
				
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
