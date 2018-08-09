package com.spaceproject.config;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.SeedComponent;

public class LandConfig {

	public SeedComponent seed;
	public PlanetComponent planet;

	public Entity ship;
	public Vector3 position;
}
