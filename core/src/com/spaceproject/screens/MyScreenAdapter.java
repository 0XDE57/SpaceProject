package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.KeyConfig;

public abstract class MyScreenAdapter extends ScreenAdapter {
    
    public static SpaceProject game;
    
    private static InputMultiplexer inputMultiplexer;
    
    // rendering objects
    public static OrthographicCamera cam;
    public static ExtendViewport viewport;
    public static SpriteBatch batch;
    public static ShapeRenderer shape;
    public static ShaderProgram shader;
    //save window size for switching between fullscreen and windowed
    private int prevWindowWidth;
    private int prevWindowHeight;
    
    private EngineConfig engineCFG;
    private KeyConfig keyCFG;
    
    
    public MyScreenAdapter() {
        Gdx.app.log(this.getClass().getSimpleName(), "ScreenAdapter Reset.");
        
        keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
        engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
        // rendering resolution
        float invScale = 1.0f / engineCFG.renderScale;
        float viewportWidth = engineCFG.viewportWidth * invScale;
        float viewportHeight = engineCFG.viewportHeight * invScale;
        prevWindowWidth = (int) viewportWidth;
        prevWindowHeight = (int) viewportHeight;
        
        cam = new OrthographicCamera();
        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        viewport = new ExtendViewport(viewportWidth, viewportHeight, cam);
        viewport.apply();
        
        resetCamera();
        
        //set this as input processor for mouse wheel scroll events
        inputMultiplexer = new InputMultiplexer();
        Gdx.input.setInputProcessor(inputMultiplexer);
        
        
        //debug
        toggleVsync();
    }
    
    @Override
    public void render(float delta) {
        //fullscreen toggle
        if (Gdx.input.isKeyJustPressed(keyCFG.fullscreen)) {
            toggleFullscreen();
        }
        
        //vsync toggle
        if (Gdx.input.isKeyJustPressed(keyCFG.vsync)) {
            toggleVsync();
        }
    }
    
    public static InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }
    
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        Gdx.app.log(this.getClass().getSimpleName(), "resize: " + width + ", " + height);
    }
    
    /**
     * Switch between fullscreen and windowed mode.
     */
    public void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            //set window to previous window size
            Gdx.graphics.setWindowedMode(prevWindowWidth, prevWindowHeight);
            Gdx.app.log(this.getClass().getSimpleName(), "Set to windowed.");
        } else {
            //save window size
            prevWindowWidth = Gdx.graphics.getWidth();
            prevWindowHeight = Gdx.graphics.getHeight();
            
            //set to fullscreen
            if (Gdx.graphics.supportsDisplayModeChange()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                Gdx.app.log(this.getClass().getSimpleName(), "Set to fullscreen.");
            } else {
                Gdx.app.log(this.getClass().getSimpleName(), "DisplayModeChange not supported.");
            }
        }
    }
    
    /**
     * Turn vsync on or off
     */
    private void toggleVsync() {
        engineCFG.vsync = !engineCFG.vsync;
        Gdx.graphics.setVSync(engineCFG.vsync);
        Gdx.app.log(this.getClass().getSimpleName(), "vsync = " + engineCFG.vsync);
    }
    
    public static void resetCamera() {
        cam.zoom = 1;
        resetRotation();
    }
    
    public static void resetRotation() {
        cam.up.set(0, 1, 0);
        cam.rotate(0);
    }
    
    @Override
    public void dispose() {
        shape.dispose();
        batch.dispose();
    }
    
}
