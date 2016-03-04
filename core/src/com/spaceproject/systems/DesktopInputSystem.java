package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.spaceproject.screens.SpaceScreen;
import com.spaceproject.utility.MyMath;

public class DesktopInputSystem extends EntitySystem {
	
	private Engine engine;	
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;		
	}	
	
	@Override
	public void update(float delta) {	
		
		//SCREEN CONTROLS///////////////////////////////////////////////////////////////////////////////////////
		//zoom test
		if (Gdx.input.isKeyPressed(SpaceScreen.keycfg.zoomSpace))  {
			if (engine.getSystem(RenderingSystem.class).getCamZoom() >= 10f) {
				engine.getSystem(RenderingSystem.class).setZoomTarget(60);
			} else {
				engine.getSystem(RenderingSystem.class).setZoomTarget(10);
			}
		}
		if (Gdx.input.isKeyPressed(SpaceScreen.keycfg.resetZoom)) engine.getSystem(RenderingSystem.class).setZoomTarget(1);
		if (Gdx.input.isKeyPressed(SpaceScreen.keycfg.zoomCharacter)) engine.getSystem(RenderingSystem.class).setZoomTarget(0.1f);
		if (Gdx.input.isKeyPressed(SpaceScreen.keycfg.zoomOut)) engine.getSystem(RenderingSystem.class).setZoomTarget(engine.getSystem(RenderingSystem.class).getCamZoom() + 0.001f);
		if (Gdx.input.isKeyPressed(SpaceScreen.keycfg.zoomIn)) engine.getSystem(RenderingSystem.class).setZoomTarget(engine.getSystem(RenderingSystem.class).getCamZoom() - 0.001f);
		
		//fullscreen toggle
		if (Gdx.input.isKeyJustPressed(SpaceScreen.keycfg.fullscreen)) {
			engine.getSystem(RenderingSystem.class).toggleFullscreen();
		}
		
		//vsync toggle
		if (Gdx.input.isKeyJustPressed(SpaceScreen.keycfg.vsync)) {
			engine.getSystem(RenderingSystem.class).toggleVsync();
		}
	
		//PLAYER CONTROLS//////////////////////////////////////////////////////////////////////////////////////
		//make ship face mouse		
		float angle = MyMath.angleTo(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
		engine.getSystem(PlayerControlSystem.class).angleFacing = angle;
	
	
		// set multiplier to full power because a key switch is on or off
		engine.getSystem(PlayerControlSystem.class).movementMultiplier = 1;
		
		//forward
		engine.getSystem(PlayerControlSystem.class).moveForward = Gdx.input.isKeyPressed(SpaceScreen.keycfg.forward);
		//right
		engine.getSystem(PlayerControlSystem.class).moveRight	= Gdx.input.isKeyPressed(SpaceScreen.keycfg.right);
		//left
		engine.getSystem(PlayerControlSystem.class).moveLeft    = Gdx.input.isKeyPressed(SpaceScreen.keycfg.left);
		//breaks
		engine.getSystem(PlayerControlSystem.class).applyBreaks = Gdx.input.isKeyPressed(SpaceScreen.keycfg.breaks);
		
		//DEBUG instant stop
		engine.getSystem(PlayerControlSystem.class).stop = Gdx.input.isKeyJustPressed(SpaceScreen.keycfg.instantStop);
		
		//shoot
		engine.getSystem(PlayerControlSystem.class).shoot = (Gdx.input.isKeyPressed(SpaceScreen.keycfg.shoot) || Gdx.input.isTouched());
		
		//enter/exit vehicle
		engine.getSystem(PlayerControlSystem.class).changeVehicle = Gdx.input.isKeyPressed(SpaceScreen.keycfg.changeVehicle);

	}

}
