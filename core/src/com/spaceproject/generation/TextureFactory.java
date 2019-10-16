package com.spaceproject.generation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.generation.noise.OpenSimplexNoise;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Tile;
import com.spaceproject.utility.MyMath;

import java.util.ArrayList;

public class TextureFactory {
    
    static Pixmap pixmap;
    
    public static Texture createTile(Color c) {
        pixmap = new Pixmap(1, 1, Format.RGB888);
        pixmap.setColor(c);
        pixmap.fill();
        
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }
    
    public static Texture createTestTile() {
        pixmap = new Pixmap(4, 4, Format.RGB888);
        pixmap.drawPixel(0, 0, Color.RED.toIntBits());
        pixmap.drawPixel(0, 1, Color.YELLOW.toIntBits());
        pixmap.drawPixel(1, 0, Color.BLUE.toIntBits());
        pixmap.drawPixel(1, 1, Color.GREEN.toIntBits());
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }
    
    static OpenSimplexNoise opacityGen = new OpenSimplexNoise(GameScreen.getInstance().getSeed());
    static OpenSimplexNoise redGen = new OpenSimplexNoise(GameScreen.getInstance().getSeed() + 1);
    static OpenSimplexNoise blueGen = new OpenSimplexNoise(GameScreen.getInstance().getSeed() + 2);
    
    public static Texture generateSpaceBackgroundDust(int tX, int tY, int tileSize) {
        pixmap = new Pixmap(tileSize, tileSize, Format.RGBA8888);
        
        double featureSize = 100;
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                //position
                double nX = (x + (tX * tileSize)) / featureSize;
                double nY = (y + (tY * tileSize)) / featureSize;
                
                //opacity
                double opacity = opacityGen.eval(nX, nY, 0);
                opacity = (opacity * 0.5) + 0.5; //convert from range [-1:1] to [0:1]
                
                //red
                double red = redGen.eval(nX, nY, 0);
                red = (red * 0.5) + 0.5;
                
                //blue
                double blue = blueGen.eval(nX, nY, 0);
                blue = (blue * 0.5) + 0.5;
                
                //draw
                pixmap.setColor(new Color((float) red, 0, (float) blue, (float) opacity));
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y);
            }
        }
        
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }
    
    public static Texture generateSpaceBackgroundStars(int tileX, int tileY, int tileSize, float depth) {
        MathUtils.random.setSeed((long) (MyMath.getSeed(tileX, tileY) * depth));
        
        //pixmap = new Pixmap(tileSize, tileSize, Format.RGB565);
        pixmap = new Pixmap(tileSize, tileSize, Format.RGBA4444);
        
        int numStars = 300;
        for (int i = 0; i < numStars; ++i) {
            
            int newX = MathUtils.random(tileSize);
            int newY = MathUtils.random(tileSize);
            pixmap.setColor(1, 1, 1, MathUtils.random(0.1f, 1f));
            pixmap.drawPixel(newX, newY);
            //pixmap.drawPixel(newX, newY, Color.rgba8888(MathUtils.random(1), MathUtils.random(1), MathUtils.random(1), 1));
        }
		
		/*
		//DEBUG - fill tile to visualize boundaries
		pixmap.setColor(MathUtils.random(), MathUtils.random(), MathUtils.random(), 0.5f);
		pixmap.fill();
		*/
        
        //create texture and dispose pixmap to prevent memory leak
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    /**
     * Generate a unique ship.
     *
     * @param size of ship
     * @return Texture of ship
     */
    public static Texture generateShip(long seed, int size) {
        MathUtils.random.setSeed(seed);
        
        boolean debugImage = false;
        
        // generate pixmap texture
        Pixmap pixmap = new Pixmap(size, size / 2, Format.RGBA4444);
        
        int width = pixmap.getWidth() - 1;
        int height = pixmap.getHeight() - 1;
        
        // 0-----width
        // |
        // |
        // |
        // height
        
        // smallest height a ship can be (4 because player is 4 pixels)
        int minEdge = 4;
        // smallest starting point for an edge
        float initialMinimumEdge = height * 0.8f;
        // edge to create shape of ship. initialize to random starting size
        int edge = MathUtils.random((int) initialMinimumEdge, height - 1);
        
        for (int yY = 0; yY <= width; yY++) {
            // draw body
            if (yY == 0 || yY == width) {
                // if first or last position of texture, "cap" it to complete
                // the edging
                //pixmap.setColor(0.7f, 0.7f, 0.7f, 1);
                pixmap.setColor(Color.LIGHT_GRAY);
            } else {
                //pixmap.setColor(0, 0.5f, 0, 1);
                pixmap.setColor(Color.DARK_GRAY);
            }
            
            if (!debugImage) {
                pixmap.drawLine(yY, edge, yY, height - edge);
            }
            
            // draw edging
            //pixmap.setColor(0.7f, 0.7f, 0.7f, 1);
            pixmap.setColor(Color.LIGHT_GRAY);
            pixmap.drawPixel(yY, edge);// bottom edge
            pixmap.drawPixel(yY, height - edge);// top edge
            
            // generate next edge
            // beginning and end of ship have special rule to not be greater
            // than the consecutive or previous edge
            // so that the "caps" look right
            if (yY == 0) { // beginning
                ++edge;
            } else if (yY == width - 1) { // end
                --edge;
            } else { // body
                // random decide to move edge. if so, move edge either up or
                // down 1 pixel
                edge = MathUtils.randomBoolean() ? (MathUtils.randomBoolean() ? --edge
                        : ++edge)
                        : edge;
            }
            
            // keep edges within height and minEdge
            if (edge > height) {
                edge = height;
            }
            if (edge - (height - edge) < minEdge) {
                edge = (height + minEdge) / 2;
            }
        }
        
        if (debugImage) {
            // fill to see image size/visual aid---------
            //pixmap.setColor(1, 1, 1, 1);
            //pixmap.fill();
            
            // mini pins for visual aid----------------
            pixmap.setColor(1, 0, 0, 1);// red: top-right
            pixmap.drawPixel(0, 0);
            
            pixmap.setColor(0, 1, 0, 1);// green: top-left
            pixmap.drawPixel(width, 0);
            
            pixmap.setColor(0, 0, 1, 1);// blue: bottom-left
            pixmap.drawPixel(0, height);
            
            pixmap.setColor(1, 1, 0, 1);// yellow: bottom-right
            pixmap.drawPixel(width, height);
        }
        
        // create texture and dispose pixmap to prevent memory leak
        Texture t = new Texture(pixmap);
        //pixmap.dispose();//or rather don't dispose because it causing issue when trying to retrieve later

		/*if pixmap is disposed, pixmap reads bad even though the texture is still fine, TODO: investigate
		Pixmap shipMap = t.getTextureData().consumePixmap();
		for (int x = 0; x < shipMap.getWidth(); x++) {
			for (int y = 0; y < shipMap.getHeight(); y++) {

				int pixel = shipMap.getPixel(x, y);
				System.out.print(pixel+" ");
			}
			System.out.println("");
		}
		System.out.println("");
		*/
        
        return t;
    }
    
    public static Texture generateShipWingLeft(long seed, int size) {
        MathUtils.random.setSeed(seed);
        Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
        int width = pixmap.getWidth() - 1;
        int height = pixmap.getHeight() - 1;
        
        pixmap.setColor(Color.GRAY);
        //pixmap.setColor(0, 0.4f, 0, 1);
        pixmap.fillTriangle(
                0, 0,  //top-right
                0, height, //bottom-left
                width, height);//bottom-right
        //pixmap.setColor(0.6f, 0.6f, 0.6f, 1);
        pixmap.setColor(Color.DARK_GRAY);
        pixmap.drawLine(0, 0, width, height);
        pixmap.drawLine(0, 0, 0, height);
        
        
        boolean debug = false;
        if (debug) {
            pixmap.setColor(1, 0, 0, 1);// red: top-right
            pixmap.drawPixel(0, 0);
            
            pixmap.setColor(0, 1, 0, 1);// green: top-left
            pixmap.drawPixel(width, 0);
            
            pixmap.setColor(0, 0, 1, 1);// blue: bottom-left
            pixmap.drawPixel(0, height);
            
            pixmap.setColor(1, 1, 0, 1);// yellow: bottom-right
            pixmap.drawPixel(width, height);
        }
        
        return new Texture(pixmap);
    }
    
    public static Texture FlipTexture(Texture originalTex, boolean flipX, boolean flipY) {
        Pixmap pixmap = new Pixmap(originalTex.getWidth(), originalTex.getHeight(), Format.RGBA4444);
        Pixmap orig = originalTex.getTextureData().consumePixmap();
        for (int y = 0; y < originalTex.getHeight(); y++) {
            for (int x = 0; x < originalTex.getWidth(); x++) {
                pixmap.drawPixel(
                        flipX ? originalTex.getWidth() - 1 - x : x,
                        flipY ? originalTex.getHeight() - 1 - y : y,
                        orig.getPixel(x, y));
            }
        }
        return new Texture(pixmap);
    }
    
    
    public static Texture combineShip(Texture body, Texture leftWing) {
        int wingOffsetX = 1;
        int wingOffsetY = 4;
        //int totalWidth = body.getWidth() + (leftWing.getWidth()-wingOffsetX*2);
        int totalHeight = body.getHeight() + ((leftWing.getHeight() - wingOffsetY) * 2);
        Pixmap pixmap = new Pixmap(body.getWidth(), totalHeight, Format.RGBA4444);
        Texture rightWing = FlipTexture(leftWing, false, true);
        pixmap.drawPixmap(leftWing.getTextureData().consumePixmap(), wingOffsetX, 0);
        pixmap.drawPixmap(rightWing.getTextureData().consumePixmap(), wingOffsetX, totalHeight - rightWing.getHeight());
        pixmap.drawPixmap(body.getTextureData().consumePixmap(), 0, (totalHeight / 2) - (body.getHeight() / 2));
        
        
        int width = pixmap.getWidth() - 1;
        int height = pixmap.getHeight() - 1;
        boolean debug = false;
        if (debug) {
            pixmap.setColor(1, 0, 0, 1);// red: top-right
            pixmap.drawPixel(0, 0);
            
            pixmap.setColor(0, 1, 0, 1);// green: top-left
            pixmap.drawPixel(width, 0);
            
            pixmap.setColor(0, 0, 1, 1);// blue: bottom-left
            pixmap.drawPixel(0, height);
            
            pixmap.setColor(1, 1, 0, 1);// yellow: bottom-right
            pixmap.drawPixel(width, height);
        }
        
        return new Texture(pixmap);
    }
    
    public static Texture generateShipUnderSide(Texture shipTop) {
        Pixmap pixmap = new Pixmap(shipTop.getWidth(), shipTop.getHeight(), Format.RGBA4444);
        Pixmap orig = shipTop.getTextureData().consumePixmap();
        for (int y = 0; y < shipTop.getHeight(); y++) {
            for (int x = 0; x < shipTop.getWidth(); x++) {
                int color = orig.getPixel(x, y);
                if (color != 0) {
                    Color c = new Color(color).sub(0.2f, 0.2f, 0.2f, 0f);
                    pixmap.drawPixel(x, y, Color.rgba8888(c));
                }
            }
        }
        
        return new Texture(pixmap);
    }
    
    
    public static Texture generatePlanet(int[][] tileMap, ArrayList<Tile> tiles) {
        int s = tileMap.length;
        Pixmap pixmap = new Pixmap(s, s, Format.RGBA4444);
        
        // draw circle for planet
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fillCircle(s / 2, s / 2, s / 2 - 1);
        
        //draw noise
        for (int y = 0; y < s; ++y) {
            for (int x = 0; x < s; ++x) {
                //only draw on circle
                if (pixmap.getPixel(x, y) != 0) {
                    pixmap.setColor(tiles.get(tileMap[x][y]).getColor());
                    pixmap.drawPixel(x, y);
                }
            }
        }
        
        //TODO: cross-thread GLContext issue cause be texuture creation not pixmap, can we create the pixmap in noisethread?
        //play with drawing the pixmap in the thread too and then delegating the
        //"No OpenGL context found in the current thread." or straight up crashes
        //https://github.com/libgdx/libgdx/wiki/Threading
        //https://www.opengl.org/wiki/OpenGL_and_multithreading
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    public static Texture generatePlanet(int mapSize, int chunkSize) {
		/*
		float scale = 100; //scale of noise = 40;
		int octaves = 4;
		float persistence = 0.68f;//0 - 1
		float lacunarity = 2.6f;//1 - x
		
		NoiseThread noise = new NoiseThread(scale, octaves, persistence, lacunarity, seed, mapSize, tiles);
		Thread createNoise = new Thread(noise);
		createNoise.start();
		*/
		/*
		
		//int mapSize = 256; //size of world
		float[][] heightMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity);
		int[][] tileMap = NoiseGen.createTileMap(heightMap, tiles);	
		int[][] pixelatedTileMap = NoiseGen.createPixelatedTileMap(tileMap, tiles);
		
		
		int s = pixelatedTileMap.length;
		Pixmap pixmap = new Pixmap(pixelatedTileMap.length, pixelatedTileMap.length, Format.RGBA4444);
		 */
        
        int size = mapSize / chunkSize;//SIZE = chunks = tileMap.length/chunkSize
        Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
        
        // draw circle for planet
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
		/*
		//draw noise
		for (int y = 0; y < s; ++y) {
			for (int x = 0; x < s; ++x) {
				//only draw on circle
				if (pixmap.getPixel(x, y) != 0) {					
					pixmap.setColor(tiles.get(pixelatedTileMap[x][y]).getColor());
					pixmap.drawPixel(x, y);
				}				
			}
		}*/
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }

	/*
	public static Texture generatePlanet(int radius) {
		OpenSimplexNoise noise = new OpenSimplexNoise();
		
		Pixmap pixmap = new Pixmap(radius * 2, radius * 2, Format.RGBA4444);
		
		double featureSize = 24;
		
		// draw circle for planet
		pixmap.setColor(1, 1, 1, 1);
		pixmap.fillCircle(radius, radius, radius - 1);
		
		//add layer of noise
		for (int y = 0; y < pixmap.getHeight(); ++y) {
			for (int x = 0; x < pixmap.getWidth(); ++x) {
				//only draw on circle
				if (pixmap.getPixel(x, y) != 0) {
					double nx = x/featureSize, ny = y/featureSize;
					double i = noise.eval(nx, ny, 0);
					i = (i * 0.5) + 0.5; //convert from range [-1:1] to [0:1]
					pixmap.setColor(new Color(0,(float)i,(float)(1-i), 1));
					pixmap.drawPixel(x, y);
				}				
			}
		}
		

		Texture t = new Texture(pixmap);
		pixmap.dispose();
		return t;
	}*/
    
    public static Texture generateStar(long seed, int radius) {
        OpenSimplexNoise noise = new OpenSimplexNoise(seed);
        
        Pixmap pixmap = new Pixmap(radius * 2, radius * 2, Format.RGBA4444);
        
        double scale = 20;//zoom
        
        // draw circle for planet
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1);
        pixmap.fillCircle(radius, radius, radius - 1);
        
        //add layer of noise
        for (int y = 0; y < pixmap.getHeight(); ++y) {
            for (int x = 0; x < pixmap.getWidth(); ++x) {
                //only draw on circle
                if (pixmap.getPixel(x, y) != 0) {
                    double nx = x / scale, ny = y / scale;
                    double i = noise.eval(nx, ny, 0);
                    i = (i * 0.5) + 0.5; //convert from range [-1:1] to [0:1]
                    if (i > 0.5f) {
                        pixmap.setColor(new Color(1, 1, 0, (float) i));
                    } else {
                        pixmap.setColor(new Color(1, 0, 0, (float) (1 - i)));
                    }
                    pixmap.drawPixel(x, y);
                }
            }
        }
        
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    public static Texture generateNoise(long seed, int size, double featureSize) {
        OpenSimplexNoise noise = new OpenSimplexNoise(seed);
        
        Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
        
        //add layer of noise
        for (int y = 0; y < pixmap.getHeight(); ++y) {
            for (int x = 0; x < pixmap.getWidth(); ++x) {
                
                double nx = x / featureSize, ny = y / featureSize;
                double i = noise.eval(nx, ny, 0);
                i = (i * 0.5) + 0.5; // convert from range [-1:1] to [0:1]
                
                pixmap.setColor(new Color((float) i, (float) i, (float) i, 1));
                pixmap.drawPixel(x, y);
                
            }
        }
        
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    public static Texture generateCharacter() {
        pixmap = new Pixmap(4, 4, Format.RGB565);
        
        //fill square
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1);
        pixmap.fill();
        
        //draw face/eyes (front of character)
        pixmap.setColor(0, 1, 1, 1);
        pixmap.drawPixel(3, 2);
        pixmap.drawPixel(3, 1);
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    public static Texture generateProjectile() {
        pixmap = new Pixmap(1, 1, Format.RGB565);// new Pixmap(size, size/2 == 0 ? 1 : size/2, Format.RGB565);
        pixmap.setColor(1, 1, 1, 1);
        //pixmap.setColor(1, 0, 0, 1);
        //pixmap.drawCircle(1,1,3);
        pixmap.fill();
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    
    public static Texture generateWall(int width, int height, Color color) {
        pixmap = new Pixmap(width, height, Format.RGB565);
        
        //fill square
        pixmap.setColor(color);
        pixmap.fill();
        pixmap.setColor(0, 0, 0, 1);
        pixmap.drawRectangle(0, 0, width, height);
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
}
