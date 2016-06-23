package com.spaceproject.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;

public class KeyConfig {

	//---player controls---
	public int forward;
	public int right;
	public int left;
	public int breaks;
	public int shoot;
	public int changeVehicle;
	
	//---screen controls---
	public int fullscreen;
	public int zoomOut;
	public int zoomIn;
	public int resetZoom;
	public int zoomCharacter;
	public int zoomSpace;
	public int rotateLeft;
	public int rotateRight;
	
	//---debug menu controls---
	public int toggleDebug;
	public int togglePos;
	public int toggleComponents;
	public int toggleBounds;
	public int toggleFPS;
	public int toggleOrbit;
	public int toggleVector;
	public int toggleMenu;
	
	
	//---temporary/test/debug controls---
	public int vsync;
	public int instantStop;
	public int createSystemAtPlayer;

	public void saveToJson() {
		
		Json json = new Json();
		json.setUsePrototypes(false);
		
		//System.out.println(json.toJson(this));			
		//check if null keys or not set, then load defaults before saving
		
		FileHandle keyFile = Gdx.files.local("controls.txt");		
		try {
			keyFile.writeString(json.toJson(this), false);			
		} catch (GdxRuntimeException ex) {
			System.out.println("Could not save file: " + ex.getMessage());
		}
	}

	public void loadDefault() {
		//player
		forward = Keys.W;
		right = Keys.D;
		left = Keys.A;
		breaks = Keys.S;
		changeVehicle = Keys.G;
		
		//screen
		fullscreen = Keys.F11;
		zoomOut = Keys.MINUS;
		zoomIn = Keys.EQUALS;
		resetZoom = Keys.PERIOD;
		zoomCharacter = Keys.SLASH;
		zoomSpace = Keys.COMMA;
		rotateLeft = Keys.LEFT_BRACKET;
		rotateRight = Keys.RIGHT_BRACKET;
		
		//debug menu
		toggleDebug = Keys.F3;
		togglePos = Keys.NUMPAD_0;
		toggleComponents = Keys.NUMPAD_1;
		toggleBounds = Keys.NUMPAD_2;
		toggleFPS = Keys.NUMPAD_3;
		toggleOrbit = Keys.NUMPAD_4;
		toggleVector = Keys.NUMPAD_5;
		toggleMenu = Keys.NUMPAD_9;
		
		//temporary/debug
		vsync = Keys.F8;
		instantStop = Keys.X;
		createSystemAtPlayer = Keys.K;
	}
	
	
}
