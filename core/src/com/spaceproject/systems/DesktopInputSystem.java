package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;

public class DesktopInputSystem extends EntitySystem {

	private ImmutableArray<Entity> players;
	
	@Override
	public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
		players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
	}	
	
	@Override
	public void update(float delta) {	
		cameraControls(delta);
		
		playerControls();
	}

	private void playerControls() {
		if (players == null || players.size() == 0) return;
		
		ControllableComponent control = Mappers.controllable.get(players.first());
		if (control == null) return;
		
		float angle = MyMath.angleTo(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), 
				Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
		control.angleFacing = angle;
		
		//movement		
		control.moveForward = Gdx.input.isKeyPressed(SpaceProject.keycfg.forward);
		control.moveRight	= Gdx.input.isKeyPressed(SpaceProject.keycfg.right);
		control.moveLeft    = Gdx.input.isKeyPressed(SpaceProject.keycfg.left);
		control.moveBack    = Gdx.input.isKeyPressed(SpaceProject.keycfg.back);
		control.movementMultiplier = 1; // set multiplier to full power because a key switch is on or off
		
		//actions
		control.shoot = (Gdx.input.isKeyPressed(SpaceProject.keycfg.shoot) || Gdx.input.isTouched());
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.changeVehicle)) {
			control.changeVehicle = true;
		}
		control.land = Gdx.input.isKeyJustPressed(SpaceProject.keycfg.land);
	}

	private static void cameraControls(float delta) {
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
