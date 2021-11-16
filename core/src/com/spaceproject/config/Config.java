package com.spaceproject.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;

public abstract class Config {
    
    private final String fileName = "config/" + this.getClass().getSimpleName() + ".json";
    
    public abstract void loadDefault();
    
    public void saveToJson() {
        saveToJson(true);
    }
    
    public void saveToJson(boolean prettyPrint) {
        Json json = new Json();
        json.setUsePrototypes(false);
        String jsonString = json.toJson(this);
        
        FileHandle keyFile = Gdx.files.local(fileName);
        
        try {
            if (prettyPrint) {
                keyFile.writeString(json.prettyPrint(jsonString), false);
            } else {
                keyFile.writeString(jsonString, false);
            }
            Gdx.app.log(this.getClass().getSimpleName(), "Saved: " + fileName);
            if (prettyPrint) {
                Gdx.app.debug(this.getClass().getSimpleName(), json.prettyPrint(jsonString));
            } else {
                Gdx.app.debug(this.getClass().getSimpleName(), jsonString);
            }
        } catch (GdxRuntimeException ex) {
            Gdx.app.error(this.getClass().getSimpleName(), "Could not save file: " + fileName + "\n" + ex.getMessage());
        }
    }
    
    public Config loadFromJson() {
        FileHandle keyFile = Gdx.files.local(fileName);
        String objectName = this.getClass().getSimpleName();
        
        try {
            if (keyFile.exists()) {
                Json json = new Json();
                json.setUsePrototypes(false);
                
                Config config = json.fromJson(this.getClass(), keyFile.readString());
                Gdx.app.log(objectName, "Loaded " + objectName + " from " + keyFile.path());
                Gdx.app.debug(objectName, json.toJson(config));
                return config;
            } else {
                Gdx.app.log(objectName, "Could not find file: " + fileName);
            }
        } catch (SerializationException e) {
            Gdx.app.error(objectName, "Could not load: " + fileName, e);
            //todo: handle missing field
            //com.badlogic.gdx.utils.SerializationException: Field not found: scale
            //if (e.detailMessage.contains(Field not found:) {
            //  extract field
            //  load default (badField = new Config.loadDefault().badField)
            //}
            //if (extra field) {
            //  dont load field?
        } catch (Exception e) {
            Gdx.app.error(objectName, "Could not load: " + fileName, e);
        }
        
        return null;
    }
    
}
