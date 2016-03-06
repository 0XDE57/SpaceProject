package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.TextureFactory;

public class TestNoiseScreen extends ScreenAdapter {
	
	SpriteBatch batch = new SpriteBatch();
	Texture noise;
	double featureSize = 20;
	long seed;
	int size = 512;
	
	public TestNoiseScreen(SpaceProject spaceProject) {
		seed = MathUtils.random(Long.MAX_VALUE);
		noise = TextureFactory.generateNoise(seed, size, featureSize);
	}

	public void render(float delta) {
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		batch.begin();
		batch.draw(noise, Gdx.graphics.getWidth()/2-noise.getWidth()/2, Gdx.graphics.getHeight()/2-noise.getHeight()/2);
		batch.end();
		
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
			seed = MathUtils.random(Long.MAX_VALUE);
			noise = TextureFactory.generateNoise(seed, size, featureSize);
		}
		
		if (Gdx.input.isKeyPressed(Keys.EQUALS)) {
			noise = TextureFactory.generateNoise(seed, size, ++featureSize);
		}
		if (Gdx.input.isKeyPressed(Keys.MINUS)) {
			noise = TextureFactory.generateNoise(seed, size, --featureSize);
		}
	}
	
	public void resize(int width, int height) {
		Gdx.app.log("screen", width + ", " + height);
	}

	public void dispose() { }

	public void hide() { }

	public void pause() { }

	public void resume() { }
	
}
