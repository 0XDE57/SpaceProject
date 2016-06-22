package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class PlanetComponent implements Component {
	private static long IDGen = 0;
	
	public PlanetComponent() {
		id = IDGen++;
	}
	
	public long id;
	
	public int mapSize;
	public long seed;	
	
	public float scale;
	public int octaves;
	public float persistence;
	public float lacunarity;

}
