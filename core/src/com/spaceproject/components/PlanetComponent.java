package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class PlanetComponent implements Component {
	private static long IDGen = 0;
	
	public PlanetComponent() {
		tempGenID = IDGen++;
	}
	
	public final long tempGenID;//sequential, used for texture gen, should not be used as unique identifier

	public int mapSize;
	public float scale;
	public int octaves;
	public float persistence;
	public float lacunarity;

}
