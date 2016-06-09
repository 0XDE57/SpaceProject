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

class ColorProfile {
	int colorBoxX;
	int colorBoxY;
	int colorWidth;
	int colorHeight;
	
	int buttonSize = 15;
	int buttonPadding = 5;
	
	int selectedColorID = -1;//-1 = none selected
	int lastSelectedID = -1;
	
	ArrayList<Tile> tiles = new ArrayList<Tile>();
	
	Comparator<Tile> tileCompare = new Comparator<Tile>() {
		@Override
		public int compare(Tile tileA, Tile tileB) {
			return (int)Math.signum(tileB.getHeight() - tileA.getHeight());
		}
	};
	
	public ColorProfile(int x, int y, int width, int height) {
		colorBoxX = x;
		colorBoxY = y;
		colorWidth = width;
		colorHeight = height;
	}
	
	public void add(Tile t) {
		tiles.add(t);
		tiles.sort(tileCompare);
	}
	
	public void draw(ShapeRenderer shape) {
		// draw color profile UI tool
		shape.setColor(Color.BLACK);
		shape.rect(colorBoxX - 1, colorBoxY - 1, colorWidth + 2, colorHeight + 2);
		for (int i = 0; i < tiles.size(); i++) {
			Tile t = tiles.get(i);
			shape.setColor(t.getColor());

			// draw color column
			if (i == 0) {
				shape.rect(colorBoxX, colorBoxY + colorHeight, colorWidth, -t.getHeight() * colorHeight);
			} else {
				shape.rect(colorBoxX, colorBoxY + t.getHeight() * colorHeight, colorWidth, -t.getHeight() * colorHeight);
			}

			// draw button
			shape.rect(colorBoxX - buttonSize - buttonPadding, colorBoxY + t.getHeight() * colorHeight, buttonSize, -buttonSize);
			//shape.setColor(Color.BLACK);
			//shape.rect(colorBoxX + colorWidth, colorBoxY + t.getHeight() * colorHeight, -buttonPadding-buttonSize-colorWidth, -1);
		}
		
		int pad = 30;
		int size = 50;
		shape.setColor(Color.BLACK);
		shape.rect(colorBoxX - size - pad - 1, colorBoxY + colorHeight/2 - size/2 -1, size+2, size+2);
		if (lastSelectedID != -1) {
			for (int j = 0; j < tiles.size(); j++) {
				if (tiles.get(j).getID() == lastSelectedID) {
					shape.setColor(tiles.get(j).getColor());
					break;
				}
			}
			shape.rect(colorBoxX - size - pad, colorBoxY + colorHeight/2 - size/2, size, size);
		}
		
	}
	
	public void draw(SpriteBatch batch, BitmapFont font) {
		for (int i = 0; i < tiles.size(); i++) {
			Tile t = tiles.get(i);		
			//MyMath.round(t.getHeight(),3) + ": " + 
			String height = String.format("%-5s", MyMath.round(t.getHeight(),3)).replace(' ', '0');
			font.draw(batch, height + " " + t.getName(), colorBoxX + colorWidth + 5, colorBoxY + t.getHeight()*colorHeight);
		}
	}
	
	public void update() {
		// select color to modify
		if (Gdx.input.isTouched() && selectedColorID == -1) {
			for (int i = 0; i < tiles.size(); i++) {
				Tile t = tiles.get(i);
				if (Gdx.input.getX() > colorBoxX - buttonSize - buttonPadding
						&& Gdx.input.getX() < colorBoxX - buttonPadding
						&& Gdx.graphics.getHeight() - Gdx.input.getY() < colorBoxY + t.getHeight() * colorHeight
						&& Gdx.graphics.getHeight() - Gdx.input.getY() > colorBoxY + t.getHeight() * colorHeight
								- buttonSize) {

					selectedColorID = t.getID();
					lastSelectedID = t.getID();
					break;
				}
				
				if (Gdx.input.getX() > colorBoxX && Gdx.input.getX() < colorBoxX + colorWidth
						&& Gdx.graphics.getHeight() - Gdx.input.getY() < colorBoxY + colorHeight
						&& Gdx.graphics.getHeight() - Gdx.input.getY() > colorBoxY + tiles.get(0).getHeight() * colorHeight
						) {
					lastSelectedID = tiles.get(0).getID();
				} else if (Gdx.input.getX() > colorBoxX && Gdx.input.getX() < colorBoxX + colorWidth
						&& Gdx.graphics.getHeight() - Gdx.input.getY() < colorBoxY + t.getHeight() * colorHeight) {

					lastSelectedID = t.getID();
				}
			}
		}

		if (!Gdx.input.isTouched())
			selectedColorID = -1;

		// move selected color
		if (selectedColorID != -1) {
			Tile t = null;
			for (int j = 0; j < tiles.size(); j++) {
				if (tiles.get(j).getID() == selectedColorID) {
					t = tiles.get(j);
					break;
				}
			}

			if (t != null) {
				t.setHeight((float) (Gdx.graphics.getHeight() - Gdx.input.getY() - colorBoxY + buttonSize/2) / colorHeight);

				tiles.sort(tileCompare);
			}

		}
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
	
	ColorProfile colorProfile = new ColorProfile(500, 50, 50, 200);
	
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
		sliders.add(new Slider("lacunarity", 0, 5, new Vector2(40, 60)));
		
		
		colorProfile.add(new Tile("water", 0.41f, Color.BLUE));
		colorProfile.add(new Tile("sand", 0.5f, Color.YELLOW));
		colorProfile.add(new Tile("grass", 0.8f, Color.GREEN));
		colorProfile.add(new Tile("lava", 1f, Color.RED));
		colorProfile.add(new Tile("rock", 0.95f, Color.BROWN));
		
	}
	
	public void render(float delta) {
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		

		colorProfile.update();
		
		
		shape.begin(ShapeType.Filled);		
		
		//draw noise map
		drawMap();
			
		//draw UI tool
		colorProfile.draw(shape);
		
		shape.end();
		

		
		batch.begin();
		
		//draw 
		colorProfile.draw(batch, font);
		
		//draw feature sliders
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

	private void drawMap() {
		int mapX = 20, mapY = Gdx.graphics.getHeight() - noiseMap.length*pixelSize - 20;
		for (int y = 0; y < noiseMap.length; y++) {
			for (int x = 0; x < noiseMap.length; x++) {
				
				//pick color
				float i = noiseMap[x][y];
				for (int k = colorProfile.tiles.size()-1; k >= 0; k--) {
					Tile tile = colorProfile.tiles.get(k);
					if (i < tile.getHeight() || k == 0) {
						shape.setColor(tile.getColor());
						break;
					}
				}
				
				//draw
				shape.rect(mapX + x * pixelSize, mapY + y * pixelSize, pixelSize, pixelSize);
				
				//grayscale debug
				shape.setColor(i, i, i, i);
				shape.rect(mapX + x * pixelSize + (noiseMap.length*pixelSize), mapY + y * pixelSize, pixelSize, pixelSize);
			}
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
