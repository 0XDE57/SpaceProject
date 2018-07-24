package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class PlanetComponent implements Component {
	private static long IDGen = 0;
	
	public PlanetComponent() {
		id = IDGen++;
	}
	
	public final long id;//sequential, used for texture gen, should not be used as unique identifier
	
	public int mapSize;
	public long seed;
	
	public float scale;
	public int octaves;
	public float persistence;
	public float lacunarity;

}
