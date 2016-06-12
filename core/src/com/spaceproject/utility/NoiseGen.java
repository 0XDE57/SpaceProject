package com.spaceproject.utility;

import com.badlogic.gdx.math.MathUtils;

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
	 * TODO: scale is incorrect in 4D implementation
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
		for (int x = 0; x < size; ++x) {
			for (int y = 0; y < size; ++y) {
				map[x][y] = MyMath.inverseLerp(minNoise, maxNoise, map[x][y]);
			}
		}
		
		return map;
		
	}
}
