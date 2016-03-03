package com.spaceproject.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;

public class KeyConfig {

	public int forward;
	public int right;
	public int left;
	public int breaks;

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
		forward = Keys.W;
		right = Keys.D;
		left = Keys.A;
		breaks = Keys.S;		
	}
	
	
}
