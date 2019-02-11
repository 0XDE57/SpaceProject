package com.spaceproject.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;

public abstract class Config {

	private final String fileName = "config/" +  this.getClass().getSimpleName() + ".json";

	/*public Config() {
		loadDefault();
	}*/
	
	public abstract void loadDefault();
	
	public void saveToJson() {
		saveToJson(true);
	}
	
	public void saveToJson(boolean prettyPrint) {
		Json json = new Json();
		json.setUsePrototypes(false);

		if (prettyPrint) {
			Gdx.app.debug(this.getClass().getSimpleName(), json.prettyPrint(json.toJson(this)));
		} else {
			Gdx.app.debug(this.getClass().getSimpleName(), json.toJson(this));
		}
		
		FileHandle keyFile = Gdx.files.local(fileName);		
		try {
			if (prettyPrint) {
				keyFile.writeString(json.prettyPrint(json.toJson(this)), false);
			} else {
				keyFile.writeString(json.toJson(this), false);
			}
			Gdx.app.log(this.getClass().getSimpleName(), "Saved: " + fileName);
		} catch (GdxRuntimeException ex) {
			Gdx.app.log(this.getClass().getSimpleName(), "Could not save file: " + fileName + "\n" + ex.getMessage());
		}
	}
	
	public Config loadFromJson() {
		FileHandle keyFile = Gdx.files.local(fileName);
		if (keyFile.exists()) {
			Json json = new Json();
			json.setUsePrototypes(false);
			
			Config config = json.fromJson(this.getClass(), keyFile.readString());
			Gdx.app.log(this.getClass().getSimpleName(), "Loaded "+ this.getClass().getSimpleName() +" from json.");
			Gdx.app.log(this.getClass().getSimpleName(), json.toJson(config));
			return config;			
		} else {
			Gdx.app.log(this.getClass().getSimpleName(), fileName + " not found. Loading defaults.");
			loadDefault();
			saveToJson();
			return this;
		}
	}
}
