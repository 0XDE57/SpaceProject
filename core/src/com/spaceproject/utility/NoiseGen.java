package com.spaceproject.utility;

import java.util.ArrayList;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.Tile;


public class NoiseGen {
	
	public static float[][] generateWrappingNoise4D(long seed, int mapSize, float scale) {
		float[][] map = new float[mapSize][mapSize];	
		
		OpenSimplexNoise noise = new OpenSimplexNoise(seed);	
		
		//generate single layer map
		for (int y = 0; y < mapSize; ++y) {
			for (int x = 0; x < mapSize; ++x) {
				
				//sinX, cosX. wrap X axis
				double sx = MathUtils.sin(x * MathUtils.PI2 / mapSize) / MathUtils.PI2 * mapSize / scale;
				double cx = MathUtils.cos(x * MathUtils.PI2 / mapSize) / MathUtils.PI2 * mapSize / scale;
				//sinY, cosY. wrap Y axis
				double sy = MathUtils.sin(y * MathUtils.PI2 / mapSize) / MathUtils.PI2 * mapSize / scale;
				double cy = MathUtils.cos(y * MathUtils.PI2 / mapSize) / MathUtils.PI2 * mapSize / scale;
				
				//get 4D noise using wrapped x and y axis
				double i = noise.eval(sx, cx, sy, cy); 
				i = (i * 0.5) + 0.5; // convert from range [-1:1] to [0:1]
				map[x][y] = (float) i;
			}
		}
		return map;
	}
	
	/**
	 * Creates a noise map in a torus so the edges wrap around.
	 * Based off of https://www.youtube.com/watch?v=MRNFcywkUSA
	 * TODO: scaling is incorrect in 4D implementation: stretching/curving distortion due to curvature of torus
	 * @param seed of noise
	 * @param size of map to generate
	 * @param scale or zoom
	 * @param octaves or layers of noise
	 * @param persistence or weight of layers
	 * @param lacunarity ?
	 * @return array holding noise
	 */
	public static float[][] generateWrappingNoise4D(long seed, int size, double scale, int octaves, float persistence, float lacunarity) {
		float[][] map = new float[size][size];
		
		OpenSimplexNoise noise = new OpenSimplexNoise(seed);
	
		float minNoise = Float.MAX_VALUE;
		float maxNoise = Float.MIN_VALUE;

		for (int x = 0; x < size; ++x) {
			for (int y = 0; y < size; ++y) {
				float amplitude = 1;
				float frequency = 1;
				float noiseHeight = 0;
				
				//for each layer(octave)
				for (int oct = 0; oct < octaves; ++oct) {
					// sinX, cosX. wrap X axis
					double sx = (MathUtils.sin(x * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale) * frequency;
					double cx = (MathUtils.cos(x * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale) * frequency;
					// sinY, cosY. wrap Y axis
					double sy = (MathUtils.sin(y * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale) * frequency;
					double cy = (MathUtils.cos(y * MathUtils.PI2 / size) / MathUtils.PI2 * size / scale) * frequency;

					// eval 4D noise using wrapped x and y axis
					double n = noise.eval(sx, cx, sy, cy);
					
					//accumulate noise
					noiseHeight += n * amplitude;
					
					//increase amplitude and frequencies for layers
					amplitude *= persistence;
					frequency *= lacunarity;				
				}
							
				//set map position to final noise value
				map[x][y] = noiseHeight;
				
				//set min and max for normalization
				if (noiseHeight > maxNoise) maxNoise = noiseHeight;
				if (noiseHeight < minNoise) minNoise = noiseHeight;
			}			
		}
		
		//normalize values to range of 0 - 1
		for (int y = 0; y < size; ++y) {
			for (int x = 0; x < size; ++x) {			
				map[x][y] = MyMath.inverseLerp(minNoise, maxNoise, map[x][y]);
			}
		}
		
		return map;
		
	}
	
	public static int[][] createPixelatedTileMap(int[][] tileMap, ArrayList<Tile> tiles) {

		int chunks = tileMap.length/SpaceProject.chunkSize;
		int[][] pixelatedMap = new int[chunks][chunks];
		
		//for each chunk
		for (int cY = 0; cY < chunks; cY++) {
			for (int cX = 0; cX < chunks; cX++) {
				
				//calculate chunk position
				int chunkX = cX * SpaceProject.chunkSize;
				int chunkY = cY * SpaceProject.chunkSize;
				
				//reset chunk count
				int[] count = new int[tiles.size()];
				
				//for each tile in chunk, count occurrence of tiles within a chunk
				for (int y = chunkY; y < chunkY+SpaceProject.chunkSize; y++) {
					for (int x = chunkX; x < chunkX+SpaceProject.chunkSize; x++) {
						count[tileMap[x][y]]++;
					}
				}
				
				//set tile to highest tile count
				pixelatedMap[cX][cY] = getMaxValueIndex(count);

			}
		}
		
		return pixelatedMap;
	}

	public static int[][] createTileMap(float[][] heightMap, ArrayList<Tile> tiles) {
		int [][] tileMap = new int[heightMap.length][heightMap.length];
		for (int y = 0; y < heightMap.length; y++) {
			for (int x = 0; x < heightMap.length; x++) {
				
				//save tile index
				float i = heightMap[x][y];			
				for (int k = tiles.size()-1; k >= 0; k--) {
					Tile tile = tiles.get(k);
					if (i <= tile.getHeight() || k == 0) {
						tileMap[x][y] = k;
						break;
					}
				}
			}
		}
		return tileMap;
	}
	
	public static int getMaxValueIndex(int[] array) {
		int index = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] > array[index]) {
				index = i;
			}
		}
		return index;
	}
	
	
	/**
	 * 
	 */
	public static float[][] createShadowMap(float[][] heightMap, Vector3 lightPos) {
		float[][] map = new float[heightMap.length][heightMap.length];
		
		lightPos.x = MathUtils.clamp(lightPos.x, 0, heightMap.length-1);
		lightPos.y = MathUtils.clamp(lightPos.y, 0, heightMap.length-1);
		
		for (int y = 0; y < heightMap.length; y++) {
			for (int x = 0; x < heightMap.length; x++) {
				ArrayList<Vector2> line = brenhamline((int)lightPos.x, (int)lightPos.y, x,y);
				for (int p = 0; p < line.size(); p++) {
					Vector2 point = line.get(p);
					//if (lightPos.z lightPos. > heightMap[x][y]) 
						map[(int) point.x][(int) point.y] = 0.5f;
				}
			}
		}
		

		/*
		 * for (int y = 0; y < map.length; y++) { map[y][4] = 0.2f; for (int x =
		 * 0; x < map.length; x++) {
		 * 
		 * } }
		 */

		return map;
	}

	private static ArrayList<Vector2> brenhamline(int x1, int y1, int x2, int y2) {
		ArrayList<Vector2> points = new ArrayList<Vector2>();
		
		
		//int x1 = map.length/2;
		//int y1 = map.length/2;
		//int x2 = (int) lightPos.x;
		//int y2 = (int) lightPos.y;

		int deltaY = y2 - y1;
		int deltaX = x2 - x1;
		
		int fraction, stepX, stepY;

		if (deltaY < 0) {
			deltaY = -deltaY;
			stepY = -1;
		} else {
			stepY = 1;
		}

		if (deltaX < 0) {
			deltaX = -deltaX;
			stepX = -1;
		} else {
			stepX = 1;
		}

		deltaY *= 2;
		deltaX *= 2;

		//map[x1][y1] = 1;
		points.add(new Vector2(x1, y1));

		if (deltaX > deltaY) {
			fraction = (2 * deltaY) - deltaX;

			while (x1 != x2) {
				if (fraction >= 0) {
					y1 += stepY;
					fraction -= deltaX;
				}

				x1 += stepX;
				fraction += deltaY;

				//map[x1][y1] = 1;
				points.add(new Vector2(x1, y1));
			}
		} else {
			fraction = (2 * deltaX) - deltaY;

			while (y1 != y2) {
				if (fraction >= 0) {
					x1 += stepX;
					fraction -= deltaY;
				}

				y1 += stepY;
				fraction += deltaX;

				//map[x1][y1] = 1;
				points.add(new Vector2(x1, y1));
			}
		}
		
		return points;
	}

	static int round(float n) {
		if (n - ((int) n) >= 0.5)
			return (int) n + 1;
		return (int) n;
	}

}
