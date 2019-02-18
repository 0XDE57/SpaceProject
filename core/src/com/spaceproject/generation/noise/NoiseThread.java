package com.spaceproject.generation.noise;

import com.badlogic.gdx.Gdx;
import com.spaceproject.Tile;

import java.util.ArrayList;

public class NoiseThread implements Runnable {
	
	private volatile boolean isDone = false;
	private volatile boolean stop = false;
	
	
	//color/height
	private ArrayList<Tile> tiles;
	
	//features
	private final long seed;
	private final float scale;
	private final int octaves;
	private final float persistence;
	private final float lacunarity;
	private final int mapSize;

	private NoiseBuffer noise;
	
	
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
		//try { Thread.sleep(150000); } catch (InterruptedException e) { }//debug delay to see loading effects
		Gdx.app.log(this.getClass().getSimpleName(), "Started: [" + toString());
		long startTime = System.currentTimeMillis();
		
		noise = new NoiseBuffer();
		noise.seed = seed;

		//do work
		if (!stop) {
			//long heightTime = System.currentTimeMillis();
			noise.heightMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity);//this call consumes most(all) the time
			//Gdx.app.log(this.getClass().getSimpleName(), toString() + "heightMap complete in : " + (System.currentTimeMillis() - heightTime) + "ms.");
		}
		if (!stop) {
			//long tileTime = System.currentTimeMillis();
			noise.tileMap = NoiseGen.createTileMap(noise.heightMap, tiles);
			//Gdx.app.log(this.getClass().getSimpleName(), toString() + "tileMap complete in : " + (System.currentTimeMillis() - tileTime) + "ms.");
		}
		if (!stop) {
			//long pixelTime = System.currentTimeMillis();
			noise.pixelatedTileMap = NoiseGen.createPixelatedTileMap(noise.tileMap, tiles);
			//Gdx.app.log(this.getClass().getSimpleName(), toString() + "pixelated complete in : " + (System.currentTimeMillis() - pixelTime) + "ms.");
		}
		
		isDone = true;


		//finish
		long endTime = System.currentTimeMillis() - startTime;
		if (stop) {
			Gdx.app.log(this.getClass().getSimpleName(), toString() + " killed. " + endTime + "ms");
		} else {
			Gdx.app.log(this.getClass().getSimpleName(), toString() + " complete in : " + endTime + "ms.");
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
		if (o instanceof NoiseThread) {
			return ((NoiseThread)o).getSeed() == this.getSeed();
		}

		return false;
	}

	@Override
	public int hashCode() {
		return (int) ((seed >> 32) ^ seed); //q/4045063
	}

	@Override
	public String toString() {
		return "[Seed:" + seed + "," + mapSize + "]";
	}


}