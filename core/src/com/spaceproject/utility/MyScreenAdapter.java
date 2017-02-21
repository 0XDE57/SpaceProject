package com.spaceproject.utility;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.spaceproject.SpaceProject;

public abstract class MyScreenAdapter extends ScreenAdapter implements InputProcessor {
	
	public static SpaceProject game;
	
	//rendering resolution
	private final static float SCALE = 1f;
	private final static float INV_SCALE = 1.f / SCALE;
	private final static float VIEWPORT_WIDTH = 1280 * INV_SCALE;
	private final static float VIEWPORT_HEIGHT = 720 * INV_SCALE;

	// rendering objects
	public static OrthographicCamera cam;
	public static SpriteBatch batch;
	public static ShapeRenderer shape;
	public static ExtendViewport viewport;
	
	private static boolean vsync = true;
	
	private static float zoomTarget = 1;
	private static float zoomSpeed = 3;
	//private static float panSpeed/panTarget(lerp to entity)
	
    //save window size for switching between fullscreen and windowed
  	private static int prevWindowWidth = (int) VIEWPORT_WIDTH;
  	private static int prevWindowHeight = (int) VIEWPORT_HEIGHT;
  	
    public MyScreenAdapter() {
    	cam = new OrthographicCamera();
		batch = new SpriteBatch();
		shape = new ShapeRenderer();
		viewport = new ExtendViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, cam);
		viewport.apply();
    	
    	//reset camera
    	cam.zoom = 1;
		setZoomTarget(1);
		
		//set this as input processor for mouse wheel scroll events
		Gdx.input.setInputProcessor(this);
		
		//debug
		System.out.println("ScreenAdapter Reset.");		
		toggleVsync();
		//
    }
    
    @Override
    public void render(float delta) {
    	cam.update();   	
    	batch.setProjectionMatrix(cam.combined);
    	shape.setProjectionMatrix(cam.combined);
    	
    	//adjust zoom
    	zoomCamera(delta);
    	
    	if (Gdx.input.isButtonPressed(Buttons.MIDDLE)) {
    		cam.zoom = 1;
    		setZoomTarget(1);
    	}
    	
    }
    
    public static void changeScreen(Screen screen) {
    	Gdx.app.log("Game", "Screen changed: " + screen.getClass().getSimpleName());
    	game.setScreen(screen);
    }
   
    @Override
    public void resize(int width, int height) {
    	viewport.update(width, height);
    	Gdx.app.log("Graphics", width + ", " + height);
    }
    
    /**
	 * Switch between fullscreen and windowed mode.
	 */
    public static void toggleFullscreen() {
		if (Gdx.graphics.isFullscreen()) {
			//set window to previous window size
			Gdx.graphics.setWindowedMode(prevWindowWidth, prevWindowHeight);
			Gdx.app.log("Graphics", "Set to windowed.");
		} else {
			//save window size
			prevWindowWidth = Gdx.graphics.getWidth();
			prevWindowHeight = Gdx.graphics.getHeight();
			
			//set to fullscreen
			if (Gdx.graphics.supportsDisplayModeChange()) {
				Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
				Gdx.app.log("Graphics", "Set to fullscreen.");
			} else {
				Gdx.app.log("Graphics", "DisplayModeChange not supported.");
			}
		}
	}
    
    /**
	 * Turn vsync on or off
	 */
	public static void toggleVsync() {
		vsync = !vsync;
		Gdx.graphics.setVSync(vsync);
		Gdx.app.log("Graphics", "vsync = " + vsync);
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
	
	@Override
	public void dispose() {
		shape.dispose();
		batch.dispose();
	}

	@Override
	public boolean scrolled(int amount) {
		setZoomTarget(cam.zoom += amount / 2f);
		return false;
	}
	
	@Override
	public boolean keyDown(int keycode) { return false; }

	@Override
	public boolean keyUp(int keycode) { return false; }

	@Override
	public boolean keyTyped(char character) { return false; }

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

	@Override
	public boolean mouseMoved(int screenX, int screenY) { return false; }
}
