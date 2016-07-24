package com.spaceproject.utility;

import java.util.ArrayList;

import com.spaceproject.Tile;
import com.spaceproject.components.PlanetComponent;

public class NoiseThread implements Runnable {
	
	//threading
	public Thread thread;
	private volatile boolean isDone = false;	
	private volatile boolean isProcessed = false;
	private volatile boolean stop = false;
	
	//the texture the noise belongs to
	private long ID;
	
	//color/height
	private ArrayList<Tile> tiles;
	
	//features
	private long seed;
	private float scale;
	private int octaves;
	private float persistence;
	private float lacunarity;	
	
	//map
	private int mapSize;
	private float[][] heightMap;
	private int[][] tileMap;
	private int[][] pixelatedTileMap;

	public NoiseThread(PlanetComponent planet, ArrayList<Tile> tiles) {
		this(planet.id, planet.scale, planet.octaves, planet.persistence, planet.lacunarity, planet.seed, planet.mapSize, tiles);
	}
	
	public NoiseThread(long id, float s, int o, float p, float l, long seed, int mapSize, ArrayList<Tile> tiles) {
		this.ID = id;
		this.scale = s;
		this.octaves = o;
		this.persistence = p;
		this.lacunarity = l;
		this.seed = seed;
		this.mapSize = mapSize;
		this.tiles = tiles;
		
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		//try { Thread.sleep(15000); } catch (InterruptedException e) { }//debug delay to see loading effects
		
		System.out.println("Thread [" + ID + "] started. MapSize=" + mapSize);
		long startTime = System.currentTimeMillis();
		
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
		
		//finish
		long endTime = System.currentTimeMillis() - startTime;
		isDone = true;
		if (stop) {
			System.out.println("Thread [" + ID + "] killed. " + endTime + "ms");
		} else {
			System.out.println("Thread [" + ID + "] complete in : " + endTime + "ms. MapSize=" + mapSize);
		}
	}
	
	public long getID() {
		return ID;
	}
	
	public float[][] getHeightMap() {
		if (!isDone()) {
			return null;
		}
		return heightMap;
	}

	public int[][] getTileMap() {
		if (!isDone()) {
			return null;
		}
		return tileMap;
	}

	public int[][] getPixelatedMap() {
		if (!isDone()) {
			return null;
		}
		return pixelatedTileMap;
	}
	
	public boolean isDone() {
		return isDone && !stop;
	}
	
	public void setProcessed() {
		isProcessed = true;
	}
	
	public boolean isProcessed(){
		if (!isDone()) {
			return false;
		}
		return isProcessed;
	}

	public void stop() {
		stop = true;
		thread.interrupt();
	}
	
	public void forceStop() {
		stop();
		thread.stop();//find a safer way?
	}
}