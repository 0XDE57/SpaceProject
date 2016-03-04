package com.spaceproject.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;

public class CelestialConfig {
	
	//---Planetary system generation---	
	//number of planets
	public int minPlanets;
	public int maxPlanets;	
	//distance between planets
	public float minDist;
	public float maxDist;
	
	
	//---Star generation---
	//size of stars
	public int minStarSize;
	public int maxStarSize;
	//star rotation speed
	public float minStarRot;
	public float maxStarRot;
	
	
	//---Planet generation---
	//size of planets
	public int minPlanetSize;
	public int maxPlanetSize;
	//planets rotation speed
	public float minPlanetRot;
	public float maxPlanetRot;
	//planet orbit speed
	public float minPlanetOrbit;
	public float maxPlanetOrbit;
	
	
	public void saveToJson() {
		
		Json json = new Json();
		json.setUsePrototypes(false);
		
		FileHandle keyFile = Gdx.files.local("celestials.txt");		
		try {
			keyFile.writeString(json.toJson(this), false);			
		} catch (GdxRuntimeException ex) {
			System.out.println("Could not save file: " + ex.getMessage());
		}
	}

	public void loadDefault() {		
		minPlanets = 0;
		maxPlanets = 10;	
		minDist = 1700;
		maxDist = 2200;
		
		minStarSize = 60;
		maxStarSize = 250;
		minStarRot = 0.002f;
		maxStarRot = 0.06f;
	
		minPlanetSize = 20;
		maxPlanetSize = 200;
		minPlanetRot = 0.015f;
		maxPlanetRot = 0.09f;
		minPlanetOrbit = 0.001f;
		maxPlanetOrbit = 0.009f;
		
	}
}
