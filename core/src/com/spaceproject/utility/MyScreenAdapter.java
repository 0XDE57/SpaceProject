package com.spaceproject.utility;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public abstract class MyScreenAdapter extends ScreenAdapter {
	
	// rendering objects
	public static OrthographicCamera cam;
	public static ExtendViewport viewport;
	public static SpriteBatch batch;
	public static ShapeRenderer shape;
	
	private static boolean vsync = true;
	
	private static float zoomTarget = 1;
	private static float zoomSpeed = 3;
	//private static float panSpeed/panTarget(lerp to entity)
	
	//rendering resolution
	private final static float SCALE = 1f;
	private final static float INV_SCALE = 1.f/SCALE;
	private final static float VIEWPORT_WIDTH = 1280 * INV_SCALE;
	private final static float VIEWPORT_HEIGHT = 720 * INV_SCALE;
        
    //save window size for switching between fullscreen and windowed
  	private static int prevWindowWidth = (int) VIEWPORT_WIDTH;
  	private static int prevWindowHeight = (int) VIEWPORT_HEIGHT;
  	
  	
    public MyScreenAdapter() {
    	cam = new OrthographicCamera();
		batch = new SpriteBatch();
		shape = new ShapeRenderer();
		viewport = new ExtendViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, cam);
		viewport.apply();
    }
    
    @Override
    public void render(float delta) {
    	cam.update();   	
    	batch.setProjectionMatrix(cam.combined);
    	shape.setProjectionMatrix(cam.combined);
    	
    	//adjust zoom
    	zoomCamera(delta);
    	
    };
    
    @Override
    public void resize(int width, int height) {
    	viewport.update(width, height);
    	Gdx.app.log("graphics", width + ", " + height);
    }
    
    /**
	 * Switch between fullscreen and windowed mode.
	 */
    public static void toggleFullscreen() {
		if (Gdx.graphics.isFullscreen()) {
			//set window to previous window size
			Gdx.graphics.setWindowedMode(prevWindowWidth, prevWindowHeight);
			Gdx.app.log("graphics", "Set to windowed.");
		} else {
			//save window size
			prevWindowWidth = Gdx.graphics.getWidth();
			prevWindowHeight = Gdx.graphics.getHeight();
			
			//set to fullscreen
			if (Gdx.graphics.supportsDisplayModeChange()) {
				Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
				Gdx.app.log("graphics", "Set to fullscreen.");
			} else {
				Gdx.app.log("graphics", "DisplayModeChange not supported.");
			}
		}
	}
    
    /**
	 * Turn vsync on or off
	 */
	public static void toggleVsync() {
		vsync = !vsync;
		Gdx.graphics.setVSync(vsync);
		Gdx.app.log("graphics", "vsync = " + vsync);
	}
	
	
	/**
	 * Animate camera zoom. Will zoom camera in or out until it reaches zoomTarget.
	 * @param delta
	 */
	private static void zoomCamera(float delta) {
		if (cam.zoom != zoomTarget) {
			//zoom in/out
			float scaleSpeed = zoomSpeed * delta;		
			cam.zoom += (cam.zoom < zoomTarget) ? scaleSpeed : -scaleSpeed;

			//if zoom is close enough, just set it to target
			if (Math.abs(cam.zoom - zoomTarget) < 0.2) {
				cam.zoom = zoomTarget;
			}
		}
	}
	
	/**
	 * Set zoom for camera to animate to.
	 * @param zoom
	 */
	public static void setZoomTarget(float zoom) {
		zoomTarget = zoom;
	}
    
}
