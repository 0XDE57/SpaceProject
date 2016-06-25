package com.spaceproject.config;

public class CelestialConfig extends Config {

	//---Planetary system generation---	
	//number of planets
	public int minPlanets;
	public int maxPlanets;	
	//distance between planets
	public float minPlanetDist;
	public float maxPlanetDist;
	
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
	
	//---Point generation---
	// how many stars TRY to create(does not guarantee this many points will actually be generated)
	public int numPoints;
	// range from origin(0,0) to create points
	public int pointGenRange; 
	// minimum distance between points
	public float minPointDistance;
	//distance to check when to load or unload planets
	public float loadSystemDistance;
	
	public void loadDefault() {
		//system gen
		minPlanets = 0;
		maxPlanets = 10;	
		minPlanetDist = 1700;
		maxPlanetDist = 2200;
		
		//star gen
		minStarSize = 60;
		maxStarSize = 250;
		minStarRot = 0.002f;
		maxStarRot = 0.06f;
		
		//planet gen
		minPlanetSize = 20;
		maxPlanetSize = 200;
		minPlanetRot = 0.015f;
		maxPlanetRot = 0.09f;
		minPlanetOrbit = 0.001f;
		maxPlanetOrbit = 0.009f;
		
		// point gen
		numPoints = 150;
		pointGenRange = 400000;
		minPointDistance = maxPlanets*maxPlanetDist*6f;	
		loadSystemDistance = maxPlanets*maxPlanetDist*1.5f;
	}
	
}
