package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.spaceproject.SpaceProject;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;

public class DesktopInputSystem extends EntitySystem {
	
	private Engine engine;
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;		
	}	
	
	@Override
	public void update(float delta) {	
		cameraControls(delta);
		playerControls();	
	}

	private void playerControls() {
		//make ship face mouse		
		float angle = MyMath.angleTo(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
		engine.getSystem(PlayerControlSystem.class).angleFacing = angle;
		
		// set multiplier to full power because a key switch is on or off
		engine.getSystem(PlayerControlSystem.class).movementMultiplier = 1;
		
		//forward
		engine.getSystem(PlayerControlSystem.class).moveForward = Gdx.input.isKeyPressed(SpaceProject.keycfg.forward);
		//right
		engine.getSystem(PlayerControlSystem.class).moveRight	= Gdx.input.isKeyPressed(SpaceProject.keycfg.right);
		//left
		engine.getSystem(PlayerControlSystem.class).moveLeft    = Gdx.input.isKeyPressed(SpaceProject.keycfg.left);
		//breaks
		engine.getSystem(PlayerControlSystem.class).applyBreaks = Gdx.input.isKeyPressed(SpaceProject.keycfg.breaks);
		
		//DEBUG instant stop
		engine.getSystem(PlayerControlSystem.class).stop = Gdx.input.isKeyJustPressed(SpaceProject.keycfg.instantStop);
		
		//shoot
		engine.getSystem(PlayerControlSystem.class).shoot = (Gdx.input.isKeyPressed(SpaceProject.keycfg.shoot) || Gdx.input.isTouched());
		
		//enter/exit vehicle
		engine.getSystem(PlayerControlSystem.class).changeVehicle = Gdx.input.isKeyPressed(SpaceProject.keycfg.changeVehicle);
		
		//land on planet
		engine.getSystem(PlayerControlSystem.class).land = Gdx.input.isKeyJustPressed(Keys.T);
	}

	private void cameraControls(float delta) {
		//zoom test
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.zoomSpace))  {
			if (MyScreenAdapter.cam.zoom >= 10f) {
				MyScreenAdapter.setZoomTarget(60);
			} else {
				MyScreenAdapter.setZoomTarget(10);
			}
		}
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.resetZoom)) {
			MyScreenAdapter.setZoomTarget(1);
		}
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.zoomCharacter)) {
			MyScreenAdapter.setZoomTarget(0.1f);
		}
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.zoomOut)) {
			MyScreenAdapter.setZoomTarget(MyScreenAdapter.cam.zoom + 0.001f);
		}
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.zoomIn)) {
			MyScreenAdapter.setZoomTarget(MyScreenAdapter.cam.zoom - 0.001f);
		}
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.rotateRight)) {
			MyScreenAdapter.cam.rotate(5f * delta);
		}
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.rotateLeft)) {
			MyScreenAdapter.cam.rotate(-5f * delta);
		}
		
		//fullscreen toggle
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.fullscreen)) { 
			MyScreenAdapter.toggleFullscreen();
		}
		
		//vsync toggle
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.vsync)) {
			MyScreenAdapter.toggleVsync();
		}
	}

}
