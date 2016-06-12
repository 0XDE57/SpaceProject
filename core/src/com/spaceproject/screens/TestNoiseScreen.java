package com.spaceproject.screens;

import java.util.ArrayList;
import java.util.Collections;

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
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.NoiseGen;

class Slider {
	static Texture buttonTex = TextureFactory.createTile(new Color(0.7f, 0.7f, 0.7f, 1f));
	static Texture slideTex = TextureFactory.createTile(new Color(0.3f, 0.3f, 0.3f, 1f));
	
	int btnWidth;
	int sldWidth;
	int sldHeight;
	
	float btnX;
	private float x, y;
	private float value;
	private float min,max;
	
	public String name;
	
	public Slider(String name, float min, float max, int x, int y, int btnWidth, int sldWidth, int sldHeight) {
		this.name = name;
		this.min = min;
		this.max = max;
		
		this.x = x;
		this.y = y;

		this.btnWidth = btnWidth;
		this.sldWidth = sldWidth;
		this.sldHeight = sldHeight;
		
		setValue((max-min)/2);
	}
	
	public String toString() {
		return name + ": " + MyMath.round(getValue(),3);
	}
	
	public void setValue(float val) {
		value = MathUtils.clamp(val, min, max);		
		updateButtonPos();
	}
	
	public float getValue() {
		return value;
	}

	
	private void updateButtonPos() {
		float pos = value / (max-min) * sldWidth + x;
		btnX = MathUtils.clamp(pos, x, x + sldWidth);		
	}
	
	public boolean isMouseInSlider() {
		int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
		int mouseX = Gdx.input.getX();
		return mouseY >= y && mouseY <= y + sldHeight 
				&& mouseX >= x - btnWidth/2 && mouseX <= x + sldWidth + btnWidth;
	}

	public void draw(SpriteBatch batch, BitmapFont font) {
		batch.draw(slideTex, x - btnWidth/2, y, sldWidth + btnWidth, sldHeight);
		batch.draw(buttonTex, btnX - btnWidth/2, y, btnWidth, sldHeight);
		font.draw(batch, toString(), x, y + sldHeight/1.5f);
	}

	public boolean update() {
		float temp = getValue();
		if (Gdx.input.isTouched() && isMouseInSlider()) {		
			float value = (Gdx.input.getX() - x) / sldWidth * (max-min);		
			setValue(value);
		}
		
		return temp != getValue();		
	}

}

class ColorProfile {
	//column dimensions
	int columnX;
	int columnY;
	int colWidth;
	int colHeight;
	
	//height modification button
	int buttonSize = 15;
	int buttonPadding = 5;
	
	//tile ID's (-1 = none selected)
	int selectedColorID = -1;
	int lastSelectedID = -1;
	
	//color of tile
	Slider red, green, blue;
	
	//colors in column
	ArrayList<Tile> tiles = new ArrayList<Tile>();
	
	public ColorProfile(int x, int y, int width, int height) {
		columnX = x;
		columnY = y;
		colWidth = width;
		colHeight = height;
		
		int sldWidth = 100;
		int sldHeight = 16;
		int btnWidth = 10;
		int sldPadding = 5;
		int offsetX = sldWidth + 30;
		int offsetY = 40;
		red   = new Slider("red",   0, 1, columnX - offsetX, columnY + offsetY + (sldHeight + sldPadding)*2, btnWidth, sldWidth, sldHeight);
		green = new Slider("green", 0, 1, columnX - offsetX, columnY + offsetY + sldHeight + sldPadding, btnWidth, sldWidth, sldHeight);
		blue  = new Slider("blue",  0, 1, columnX - offsetX, columnY + offsetY , btnWidth, sldWidth, sldHeight);
	}
	
	public void add(Tile t) {
		tiles.add(t);
		Collections.sort(tiles);
	}
	
	public void draw(ShapeRenderer shape) {
		// draw color profile UI tool
		shape.setColor(Color.BLACK);
		shape.rect(columnX - 1, columnY - 1, colWidth + 2, colHeight + 2);
		for (int i = 0; i < tiles.size(); i++) {
			Tile t = tiles.get(i);
			shape.setColor(t.getColor());

			// draw color column
			if (i == 0) {
				shape.rect(columnX, columnY + colHeight, colWidth, -t.getHeight() * colHeight);
			} else {
				shape.rect(columnX, columnY + t.getHeight() * colHeight, colWidth, -t.getHeight() * colHeight);
			}

			// draw button
			shape.rect(columnX - buttonSize - buttonPadding, columnY + t.getHeight() * colHeight, buttonSize, -buttonSize);
			//shape.setColor(Color.BLACK);
			//shape.rect(colorBoxX + colorWidth, colorBoxY + t.getHeight() * colorHeight, -buttonPadding-buttonSize-colorWidth, -1);
		}
		
		//draw currently selected color
		int size = 50;
		int offsetX = 50;
		int offsetY = colHeight - size - 30;	
		shape.setColor(Color.BLACK);
		shape.rect(columnX - size - offsetX - 1, columnY + offsetY -1, size+2, size+2);
		if (lastSelectedID != -1) {
			for (int j = 0; j < tiles.size(); j++) {
				if (tiles.get(j).getID() == lastSelectedID) {
					shape.setColor(tiles.get(j).getColor());					
					break;
				}
			}
			shape.rect(columnX - size - offsetX, columnY + offsetY, size, size);
		}
		
	}
	
	public void draw(SpriteBatch batch, BitmapFont font) {
		//draw tile info
		for (int i = 0; i < tiles.size(); i++) {
			Tile t = tiles.get(i);
			font.draw(batch, t.toString(), columnX + colWidth + 5, columnY + t.getHeight()*colHeight);
		}
		
		//draw color sliders
		red.draw(batch, font);
		green.draw(batch, font);
		blue.draw(batch, font);
	}
	
	public void update() {
		//update color sliders
		if (red.update() || green.update() || blue.update()) {
			//set selected color to slider values
			for (int j = 0; j < tiles.size(); j++) {
				if (tiles.get(j).getID() == lastSelectedID) {
					Tile t = tiles.get(j);
					t.getColor().r = red.getValue();
					t.getColor().g = green.getValue();
					t.getColor().b = blue.getValue();
					break;
				}
			}
		}
		
		boolean selectionChanged = false;
		int mouseX = Gdx.input.getX();
		int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
		
		// select color to modify
		if (Gdx.input.isTouched() && selectedColorID == -1) {
			for (int i = 0; i < tiles.size(); i++) {
				
				Tile t = tiles.get(i);						
				float colorTop = columnY + t.getHeight() * colHeight;
				
				//check if mouse in button
				if (mouseX > columnX - buttonSize - buttonPadding && mouseX < columnX - buttonPadding
						&& mouseY < colorTop && mouseY > colorTop - buttonSize) {
					
					selectedColorID = t.getID();									
					lastSelectedID = t.getID();
					selectionChanged = true;
					break;
				}
				
				//check if click on color in color column
				if (mouseX > columnX && mouseX < columnX + colWidth && mouseY < columnY + colHeight 
						&& mouseY > columnY + tiles.get(0).getHeight() * colHeight) {					
					//select first tile from top if height is not at the top of the column
					lastSelectedID = tiles.get(0).getID();
					selectionChanged = true;
					
				} else if (mouseX > columnX && mouseX < columnX + colWidth && mouseY < colorTop) {
					lastSelectedID = t.getID();
					selectionChanged = true;
				}
			}
		}

		if (!Gdx.input.isTouched())
			selectedColorID = -1;

		// move selected color
		if (selectedColorID != -1) {
			for (int j = 0; j < tiles.size(); j++) {
				if (tiles.get(j).getID() == selectedColorID) {
					Tile t = tiles.get(j);
					t.setHeight((float)(mouseY - columnY + buttonSize/2) / colHeight);
					
					Collections.sort(tiles);
					break;
				}
			}
		}
		
		//set RGB sliders to selected color
		if (selectionChanged) {
			for (int j = 0; j < tiles.size(); j++) {
				if (tiles.get(j).getID() == lastSelectedID) {
					Tile t = tiles.get(j);
					red.setValue(t.getColor().r);
					green.setValue(t.getColor().g);
					blue.setValue(t.getColor().b);					
					break;
				}
			}		
		}
	}
	
}

class Tile implements Comparable<Tile> {
	private static int nextID;
	private final int id;
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

	@Override
	public int compareTo(Tile o) {
		return (int)Math.signum(o.getHeight() - this.getHeight());
	}
	
	@Override
	public String toString() {
		return String.format("%-5s", MyMath.round(getHeight(),3)).replace(' ', '0')  + " " + getName();
	}

}

public class TestNoiseScreen extends ScreenAdapter {
	
	SpriteBatch batch = new SpriteBatch();
	ShapeRenderer shape = new ShapeRenderer();
	private BitmapFont font;
	
	long seed; //make textbox
		
	int mapSize = 120;//make slider
	int pixelSize = 3;//zoom //make slider
	
	float[][] noiseMap;
	
	//feature sliders
	Slider scale, octave, persistence, lacunarity;
	
	//color picking tool
	ColorProfile colorProfile = new ColorProfile(500, 50, 50, 200);
	
	public TestNoiseScreen(SpaceProject space) {	
		
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
				
		seed = MathUtils.random(Long.MAX_VALUE);		
		
	
		int width = 200;
		int height = 30;
		int buttonWidth = 20;
		scale       = new Slider("scale",       1, 100, 40,  180, buttonWidth, width, height);//1 - x
		octave      = new Slider("octave",      1, 6,   40,  140, buttonWidth, width, height);//1 - x
		persistence = new Slider("persistence", 0, 1,   40,  100, buttonWidth, width, height);//0 - 1
		lacunarity  = new Slider("lacunarity",  0, 5,   40,   60, buttonWidth, width, height);//0 - x
		
		updateMap();
		
		colorProfile.add(new Tile("water", 0.41f, Color.BLUE));
		colorProfile.add(new Tile("sand",  0.5f,  Color.YELLOW));
		colorProfile.add(new Tile("grass", 0.8f,  Color.GREEN));
		colorProfile.add(new Tile("lava",  1f,    Color.RED));
		colorProfile.add(new Tile("rock",  0.95f, Color.BROWN));
		
		
		//scale.setValue(10);
	}
	
	
	public void render(float delta) {
		Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
				
		shape.begin(ShapeType.Filled);		
		
		//draw noise map
		drawMap();
			
		//draw UI tool
		colorProfile.draw(shape);
		
		shape.end();
		
		
		batch.begin();
		
		//draw UI tool
		colorProfile.draw(batch, font);
		
		//draw feature sliders
		scale.draw(batch, font);
		octave.draw(batch, font);
		persistence.draw(batch, font);
		lacunarity.draw(batch, font);
		
		
		font.draw(batch, "Seed: " + seed, 15, Gdx.graphics.getHeight() - 15);
		font.draw(batch, "Zoom: " + pixelSize, 15, Gdx.graphics.getHeight() - 30);
		
		batch.end();
		
		
		//update 
		colorProfile.update();
		
		
		boolean change = false;
		if (scale.update() || octave.update() 
				|| persistence.update() || lacunarity.update()){
			change = true;
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
			updateMap();
		}
			
	}


	private void updateMap() {
		double s = scale.getValue();			
		int o = (int)octave.getValue();
		float p = persistence.getValue();
		float l = lacunarity.getValue();

		noiseMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, s, o, p, l);
	}

	private void drawMap() {
		int mapX = 20, mapY = Gdx.graphics.getHeight() - noiseMap.length*pixelSize - 20;
		for (int y = 0; y < noiseMap.length; y++) {
			for (int x = 0; x < noiseMap.length; x++) {
				
				//pick color
				float i = noiseMap[x][y];
				for (int k = colorProfile.tiles.size()-1; k >= 0; k--) {
					Tile tile = colorProfile.tiles.get(k);
					if (i <= tile.getHeight() || k == 0) {
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
