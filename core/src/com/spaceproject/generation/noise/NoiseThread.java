package com.spaceproject.generation.noise;

import com.spaceproject.Tile;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.SeedComponent;

import java.util.ArrayList;

public class NoiseThread implements Runnable {

	private volatile boolean isDone = false;
	private volatile boolean stop = false;

	
	//color/height
	private ArrayList<Tile> tiles;
	
	//features
	private long seed;
	private float scale;
	private int octaves;
	private float persistence;
	private float lacunarity;
	private int mapSize;

	//TODO: move features directly into buffer or directly reference planetcomponent?

	NoiseBuffer noise;

	public NoiseThread(SeedComponent seed, PlanetComponent planet, ArrayList<Tile> tiles) {
		this(planet.scale, planet.octaves, planet.persistence, planet.lacunarity, seed.seed, planet.mapSize, tiles);
	}
	
	public NoiseThread(float s, int o, float p, float l, long seed, int mapSize, ArrayList<Tile> tiles) {
		this.scale = s;
		this.octaves = o;
		this.persistence = p;
		this.lacunarity = l;
		this.seed = seed;
		this.mapSize = mapSize;
		this.tiles = tiles;

	}

	@Override
	public void run() {
		//try { Thread.sleep(15000); } catch (InterruptedException e) { }//debug delay to see loading effects
		
		System.out.println("Started: [" + toString());
		long startTime = System.currentTimeMillis();

		float[][] heightMap = new float[0][0];
		int[][] tileMap = new int[0][0];
		int[][] pixelatedTileMap = new int[0][0];

		//do work
		if (!stop) {
			heightMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity);
		}
		if (!stop) { 
			tileMap = NoiseGen.createTileMap(heightMap, tiles);
		}
		if (!stop) { 
			pixelatedTileMap = NoiseGen.createPixelatedTileMap(tileMap, tiles);
		}

		noise = new NoiseBuffer();
		noise.seed = seed;
		noise.heightMap = heightMap;
		noise.tileMap = tileMap;
		noise.pixelatedTileMap = pixelatedTileMap;
		isDone = true;


		//finish
		long endTime = System.currentTimeMillis() - startTime;
		if (stop) {
			System.out.println(toString() + " killed. " + endTime + "ms");
		} else {
			System.out.println(toString() + " complete in : " + endTime + "ms.");
		}
	}


	public NoiseBuffer getNoise() {
		if (isDone())
			return noise;

		return null;
	}

	public long getSeed() {
		return seed;
	}
	
	public boolean isDone() {
		return isDone && !stop;
	}


	public void stop() {
		stop = true;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NoiseThread)) {
			return ((NoiseThread)o).getSeed() == this.getSeed();
		}

		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "NoiseGenerator: Seed[" + seed + "], size[" + mapSize+ "]";
	}


}