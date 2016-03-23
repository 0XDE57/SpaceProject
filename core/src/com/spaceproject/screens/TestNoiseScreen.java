package com.spaceproject.screens;

import java.util.ArrayList;

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
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.NoiseGen;

class Slider {
	int btnWidth = 20;
	int sldWidth = 200;
	int sldHeight = 30;
	
	Vector2 pos;
	
	float btnX;
	private float min;
	private float max;	
	
	public String name;
	
	public Slider(String name, float min, float max, Vector2 position){
		this.name = name;
		this.min = min;
		this.max = max;
		this.pos = position;
		btnX = pos.x + sldWidth/2; //half bar
	}
	
	public String toString() {
		return name + " (" + btnX + "):" + getValue();
	}
	
	public float getValue() {
		return (float) MyMath.round((btnX - pos.x) / sldWidth * (max - min) + 1, 2);
	}

	public void setPos(int x) {
		btnX = MathUtils.clamp(x, pos.x, pos.x + sldWidth);
		
	}

	
}

public class TestNoiseScreen extends ScreenAdapter {
	
	SpriteBatch batch = new SpriteBatch();
	private BitmapFont font;
	
	Texture buttonTex = TextureFactory.createTile(new Color(0.7f, 0.7f, 0.7f, 1f));
	Texture slideTex = TextureFactory.createTile(new Color(0.3f, 0.3f, 0.3f, 1f));
	
	int mapSize = 120;
	float pixelSize = 3.0f;
	
	long seed;
	Texture noise;
	
	
	ArrayList<Slider> sliders = new ArrayList<Slider>();
	
	public TestNoiseScreen(SpaceProject space) {
		seed = MathUtils.random(Long.MAX_VALUE);
		noise = createNoiseMapTex(NoiseGen.generateWrappingNoise4D(seed, mapSize, 40, 4, 1, 2));
		
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
		
		font.setColor(1, 1, 1, 1);
		
	
		//double scale = 40;//30 - 100
		//int octaves = 4;
		//float persistence = 0.5f;//0 - 1
		//float lacunarity = 2;//1 - x
		sliders.add(new Slider("scale", 1, 80, new Vector2(40,  180)));
		sliders.add(new Slider("octave", 1, 4, new Vector2(40, 140)));
		sliders.add(new Slider("persistence", 0, 1, new Vector2(40, 100)));
		sliders.add(new Slider("lacunarity", 0, 10, new Vector2(40, 60)));
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

		batch.draw(noise, (Gdx.graphics.getWidth()/2) - (noise.getWidth()/2), (Gdx.graphics.getHeight()/2) - (noise.getHeight()/2),
				   0, 0,
				   mapSize, mapSize,
				   pixelSize, pixelSize,
				   0,
				   0, 0, mapSize, mapSize, false, false);
		
		
		for (Slider slide : sliders) {
			batch.draw(slideTex, slide.pos.x - slide.btnWidth/2, slide.pos.y, slide.sldWidth + slide.btnWidth, slide.sldHeight);
			batch.draw(buttonTex, slide.btnX - slide.btnWidth/2, slide.pos.y, slide.btnWidth, slide.sldHeight);
			font.draw(batch, slide.toString(), slide.pos.x, slide.pos.y + slide.sldHeight/2);
		}
		
		font.draw(batch, "Seed: " + seed, 15, Gdx.graphics.getHeight() - 15);
		font.draw(batch, "Zoom: " + pixelSize, 15, Gdx.graphics.getHeight() - 30);
		
		batch.end();
		
		boolean change = false;
		if (Gdx.input.isTouched()) {
			for (Slider s : sliders) {
				if (isMouseInSlider(s)) {
					s.setPos(Gdx.input.getX());
					change = true;
				}
			}
		}
		

		
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
		
		if (change) {
			//double scale = sliders.indexOf(o -> o.getValue());  // indexOf((e) -> (e.))
			double scale = 0;			
			int octaves = 0;
			float persistence = 0;
			float lacunarity = 0;
			for (Slider s : sliders) {
				switch (s.name) {
					case "scale": 
						scale = s.getValue(); break;
					case "octave": 
						octaves = (int)s.getValue(); break;
					case "persistence":
						persistence = s.getValue(); break;
					case "lacunarity":
						lacunarity = s.getValue(); break;
				}
			}
			System.out.println(scale + ",  " + octaves + ", " + persistence + ", " + lacunarity);
			
			noise = createNoiseMapTex(NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity)); //noise = generateNoise3(seed, size, scale, zed);
		}
			
	}

	private static boolean isMouseInSlider(Slider s) {
		float yTop = Gdx.graphics.getHeight() - Gdx.input.getY();
		float yBot = Gdx.graphics.getHeight() - Gdx.input.getY();
		return yTop >= s.pos.y && yBot <= s.pos.y + s.sldHeight &&  Gdx.input.getX() >= s.pos.x- s.btnWidth/2 && Gdx.input.getX() <= s.pos.x + s.sldWidth + s.btnWidth;
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
