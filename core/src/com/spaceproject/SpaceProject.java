package com.spaceproject;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.*;

public class SpaceProject extends Game {

	public static long SEED = 4; //test seed
	
	public static CelestialConfig celestcfg;
	public static KeyConfig keycfg;

	public static Vector3 landedPlanet;//hacky temporary way to save planet player landed on
	
	@Override
	public void create() {
		
		//load values for things like key mapping, settings, default values for generation
		loadConfigs();
		
		//setScreen(new TestShipGenerationScreen(this));
		//setScreen(new TestNoiseScreen(this));
		
		setScreen(new SpaceScreen(this, new Vector3()));
		//setScreen(new WorldScreen(this));
		
		//setScreen(new MainMenuScreen(this));
	}
	
	private static void loadConfigs() {
		//KEYS
		keycfg = new KeyConfig();
		keycfg.loadDefault();
		keycfg.saveToJson();
		
		/*
		FileHandle keyFile = Gdx.files.local("controls.txt");
		if (keyFile.exists()) {
			Json json = new Json();
			json.setUsePrototypes(false);
			
			keycfg = json.fromJson(KeyConfig.class, keyFile.readString());
			System.out.println("Loaded keys from json: " + json.toJson(keycfg));
		} else {
			keycfg = new KeyConfig();
			keycfg.loadDefault();
			//keycfg.saveToJson();
			System.out.println("No key file found. Loaded defaults.");
		}*/
		
		
		//CELESTIAL OBJECTS
		celestcfg = new CelestialConfig();
		celestcfg.loadDefault();
		celestcfg.saveToJson();
	}		
	
}
