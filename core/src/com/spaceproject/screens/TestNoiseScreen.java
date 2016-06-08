package com.spaceproject.screens;

import java.util.ArrayList;
import java.util.Comparator;

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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
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
	
	public Slider(String name, float min, float max, Vector2 position) {
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
	
	public static boolean isMouseInSlider(Slider s) {
		float yTop = Gdx.graphics.getHeight() - Gdx.input.getY();
		float yBot = Gdx.graphics.getHeight() - Gdx.input.getY();
		return yTop >= s.pos.y && yBot <= s.pos.y + s.sldHeight 
				&& Gdx.input.getX() >= s.pos.x - s.btnWidth/2 
				&& Gdx.input.getX() <= s.pos.x + s.sldWidth + s.btnWidth;
	}


	
}

class Tile {
	private static int nextID;
	private int id;
	private String name;
	private float height;
	private Color color;
	
	public Tile(String name, float height, Color color) {
		this.name = name;
		this.height = height;
		this.color = color;
		
		id = nextID++;
	}
	
	public int getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public float getHeight() {
		return height;
	}
	
	public void setHeight(float height) {
		this.height = MathUtils.clamp(height, 0, 1);
	}

}

public class TestNoiseScreen extends ScreenAdapter {
	
	SpriteBatch batch = new SpriteBatch();
	ShapeRenderer shape = new ShapeRenderer();
	private BitmapFont font;
	
	Texture buttonTex = TextureFactory.createTile(new Color(0.7f, 0.7f, 0.7f, 1f));
	Texture slideTex = TextureFactory.createTile(new Color(0.3f, 0.3f, 0.3f, 1f));
	
	int mapSize = 120;
	int pixelSize = 3;
	
	long seed;
	Texture noise;
	float[][] noiseMap;
	
	
	ArrayList<Slider> sliders = new ArrayList<Slider>();
	
	
	int colorBoxX = 500;
	int colorBoxY = 50;
	int colorWidth = 50;
	int colorHeight = 200;	
	int buttonSize = 15;
	int buttonPadding = 5;
	
	int selectedColorID = -1;//-1 = none selected
	
	ArrayList<Tile> colorProfile = new ArrayList<Tile>();
	
	Comparator<Tile> tileCompare = new Comparator<Tile>() {
		@Override
		public int compare(Tile tileA, Tile tileB) {
			return (int)Math.signum(tileB.getHeight() - tileA.getHeight());
		}
	};
	
	public TestNoiseScreen(SpaceProject space) {
		seed = MathUtils.random(Long.MAX_VALUE);
		//noise = createNoiseMapTex(NoiseGen.generateWrappingNoise4D(seed, mapSize, 40, 4, 1, 2));
		
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
		
		font.setColor(1, 1, 1, 1);
		
		noiseMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, 40, 4, 1, 2);
	
		//double scale = 40;//30 - 100
		//int octaves = 4;
		//float persistence = 0.5f;//0 - 1
		//float lacunarity = 2;//1 - x
		sliders.add(new Slider("scale", 1, 100, new Vector2(40,  180)));
		sliders.add(new Slider("octave", 1, 6, new Vector2(40, 140)));
		sliders.add(new Slider("persistence", 0, 1, new Vector2(40, 100)));
		sliders.add(new Slider("lacunarity", 0, 10, new Vector2(40, 60)));
		
		
		colorProfile.add(new Tile("test", 0.75f, Color.BLUE));
		colorProfile.add(new Tile("test1", 0.5f, Color.YELLOW));
		colorProfile.add(new Tile("test2", 1f, Color.GREEN));
		colorProfile.add(new Tile("test4", 0.9f, Color.RED));
		colorProfile.sort(tileCompare);
			
	}
	
	public void render(float delta) {
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		
		////////////////////////////////////////////////////////////////////////////////////
		
		
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.WHITE);
		
		//draw noise map in color
		int mapX = 20, mapY = Gdx.graphics.getHeight() - noiseMap.length*pixelSize - 20;
		for (int y = 0; y < noiseMap.length; y++) {
			for (int x = 0; x < noiseMap.length; x++) {
				float i = noiseMap[x][y];
				for (int k = colorProfile.size()-1; k >= 0; k--) {
					Tile tile = colorProfile.get(k);
					if (i < tile.getHeight() || i == 0) {
						shape.setColor(tile.getColor());
						break;
					}
				}
				
				shape.rect(mapX + x * pixelSize, mapY + y * pixelSize, pixelSize, pixelSize);
			}
		}
		
		//draw noise map in grayscale
		for (int y = 0; y < noiseMap.length; y++) {
			for (int x = 0; x < noiseMap.length; x++) {
				float i = noiseMap[x][y];
				shape.setColor(i, i, i, i);
				shape.rect(mapX + x * pixelSize + (noiseMap.length*pixelSize), mapY + y * pixelSize, pixelSize, pixelSize);
			}
		}
			
		//for (Tile t : colorProfile) System.out.println(t.getHeight());
		//for (int k = colorProfile.size()-1; k >= 0; k--) System.out.println(colorProfile.get(k).getHeight());
		
		//draw color profile UI tool
		shape.rect(colorBoxX-1, colorBoxY-1, colorWidth+2, colorHeight+2);		
		for (int i = 0; i < colorProfile.size(); i++) {
			Tile t = colorProfile.get(i);
			shape.setColor(t.getColor());
			
			//draw color column
			if (i == 0) {
				shape.rect(colorBoxX, colorBoxY + colorHeight, colorWidth, -t.getHeight()*colorHeight);
			} else {
				shape.rect(colorBoxX, colorBoxY + t.getHeight()*colorHeight, colorWidth, -t.getHeight()*colorHeight);
			}
			
			//draw button
			shape.rect(colorBoxX - buttonSize - buttonPadding, colorBoxY + t.getHeight()*colorHeight, buttonSize, -buttonSize);
		}
		shape.end();
		
		//select color to modify
		if (Gdx.input.isTouched() && selectedColorID == -1) {
			for (int i = 0; i < colorProfile.size(); i++) {
				Tile t = colorProfile.get(i);
				if (Gdx.input.getX() > colorBoxX-buttonSize-buttonPadding && Gdx.input.getX() < colorBoxX - buttonPadding
						&& Gdx.graphics.getHeight() - Gdx.input.getY() < colorBoxY + t.getHeight()*colorHeight
						&& Gdx.graphics.getHeight() - Gdx.input.getY() > colorBoxY + t.getHeight()*colorHeight - buttonSize) {
					
					selectedColorID = t.getID();
					break;
				}			
			}
		}
		
		if (!Gdx.input.isTouched())
			selectedColorID = -1;
		
		//move selected color
		if (selectedColorID != -1) {
			Tile t = null;
			for (int j = 0; j < colorProfile.size(); j++) {
				if (colorProfile.get(j).getID() == selectedColorID) {
					t = colorProfile.get(j);
					break;
				}
			}
			
			if (t != null) {
				t.setHeight((float)(Gdx.graphics.getHeight() - Gdx.input.getY() - colorBoxY + buttonSize/2)/colorHeight);
			
				colorProfile.sort(tileCompare);
			}
			
		}
		//////////////////////////////////////////////////////////////////////////////
		
		batch.begin();
		
		//batch.draw(noise, Gdx.graphics.getWidth()/2-noise.getWidth()/2, Gdx.graphics.getHeight()/2-noise.getHeight()/2);		
		//batch.draw(noise, 0, 0);
		//batch.draw(noise, mapSize, 0);
		//batch.draw(noise, 0, mapSize);
		//batch.draw(noise, mapSize, mapSize);

		/*
		batch.draw(noise, 20, Gdx.graphics.getHeight() - noise.getHeight()*pixelSize - 20,
				   0, 0,
				   mapSize, mapSize,
				   pixelSize, pixelSize,
				   0,
				   0, 0, mapSize, mapSize, false, false);
		*/
		
		for (Slider slide : sliders) {
			batch.draw(slideTex, slide.pos.x - slide.btnWidth/2, slide.pos.y, slide.sldWidth + slide.btnWidth, slide.sldHeight);
			batch.draw(buttonTex, slide.btnX - slide.btnWidth/2, slide.pos.y, slide.btnWidth, slide.sldHeight);
			font.draw(batch, slide.toString(), slide.pos.x, slide.pos.y + slide.sldHeight/2);
		}
		
		for (int i = 0; i < colorProfile.size(); i++) {
			Tile t = colorProfile.get(i);			
			font.draw(batch, MyMath.round(t.getHeight(),3) + ": " + t.getName(), 
					colorBoxX + colorWidth + 5, colorBoxY + t.getHeight()*colorHeight);
		}
		
		font.draw(batch, "Seed: " + seed, 15, Gdx.graphics.getHeight() - 15);
		font.draw(batch, "Zoom: " + pixelSize, 15, Gdx.graphics.getHeight() - 30);
		
		batch.end();
		
		boolean change = false;
		if (Gdx.input.isTouched()) {
			for (Slider s : sliders) {
				if (Slider.isMouseInSlider(s)) {
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
			//System.out.println(scale + ",  " + octaves + ", " + persistence + ", " + lacunarity);		
			//noise = createNoiseMapTex(NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity)); //noise = generateNoise3(seed, size, scale, zed);
			
			noiseMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity);
		}
			
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
