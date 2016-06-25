package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.spaceproject.SpaceProject;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;

public class DesktopInputSystem extends EntitySystem {

	@Override
	public void update(float delta) {	
		cameraControls(delta);
		playerControls();	
	}

	private void playerControls() {
		///////////////////
		/////DIRECTION/////
		///////////////////
		//make ship face mouse		
		float angle = MyMath.angleTo(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), 
				Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
		PlayerControlSystem.angleFacing = angle;
		
		//////////////////
		/////MOVEMENT/////
		//////////////////
		// set multiplier to full power because a key switch is on or off
		PlayerControlSystem.movementMultiplier = 1;
		PlayerControlSystem.moveForward = Gdx.input.isKeyPressed(SpaceProject.keycfg.forward);//forward
		PlayerControlSystem.moveRight	= Gdx.input.isKeyPressed(SpaceProject.keycfg.right);//right
		PlayerControlSystem.moveLeft    = Gdx.input.isKeyPressed(SpaceProject.keycfg.left);//left
		PlayerControlSystem.applyBreaks = Gdx.input.isKeyPressed(SpaceProject.keycfg.breaks);//breaks	
		PlayerControlSystem.stop 		= Gdx.input.isKeyPressed(SpaceProject.keycfg.instantStop);//DEBUG instant stop
		
		/////////////////
		/////ACTIONS/////
		/////////////////
		//shoot
		PlayerControlSystem.shoot = (Gdx.input.isKeyPressed(SpaceProject.keycfg.shoot) || Gdx.input.isTouched());
		
		//enter/exit vehicle
		PlayerControlSystem.changeVehicle = Gdx.input.isKeyPressed(SpaceProject.keycfg.changeVehicle);
		
		//land on planet
		PlayerControlSystem.land = Gdx.input.isKeyJustPressed(SpaceProject.keycfg.land);
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
