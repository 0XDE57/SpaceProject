package com.spaceproject.config;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.PlanetComponent;

public class LandConfig {

	public PlanetComponent planet;
	public Entity ship;
	public long shipSeed;
	public Vector3 position;
}
