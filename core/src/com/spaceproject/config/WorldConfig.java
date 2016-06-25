package com.spaceproject.config;

public class WorldConfig extends Config {

	/*
	minMapSize, maxMapSize
	minScale, maxScale
	minOctaves, maxOctave
	minPersistence, maxPersistence
	minLacunarity, maxLacunarity
	*/
	
	public int tileSize;
	public int chunkSize;
	public int surround;
	
	@Override
	public void loadDefault() {
		tileSize = 32;
		
	}
}
