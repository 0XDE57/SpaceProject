package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

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
		if (Gdx.input.isKeyPressed(Keys.COMMA))  {
			if (engine.getSystem(RenderingSystem.class).getCam().zoom >= 10) {
				engine.getSystem(RenderingSystem.class).zoom(60);
				engine.getSystem(RenderingSystem.class).getCam().zoom = 50;
			} else {
				engine.getSystem(RenderingSystem.class).zoom(10);
			}
		}
		if (Gdx.input.isKeyPressed(Keys.PERIOD)) engine.getSystem(RenderingSystem.class).zoom(1);
		if (Gdx.input.isKeyPressed(Keys.SLASH)) engine.getSystem(RenderingSystem.class).zoom(0.1f);
		if (Gdx.input.isKeyPressed(Keys.MINUS)) engine.getSystem(RenderingSystem.class).zoom(engine.getSystem(RenderingSystem.class).getCam().zoom += 0.001);
		if (Gdx.input.isKeyPressed(Keys.EQUALS)) engine.getSystem(RenderingSystem.class).zoom(engine.getSystem(RenderingSystem.class).getCam().zoom -= 0.001);
		
		//fullscreen toggle
		if (Gdx.input.isKeyJustPressed(Keys.F11)) {
			engine.getSystem(RenderingSystem.class).toggleFullscreen();
		}
		
		//vsync toggle
		if (Gdx.input.isKeyJustPressed(Keys.F8)) {
			engine.getSystem(RenderingSystem.class).toggleVsync();
		}
	
		//PLAYER CONTROLS//////////////////////////////////////////////////////////////////////////////////////
		//make ship face mouse		
		engine.getSystem(PlayerControlSystem.class).pointTo(Gdx.input.getX(), Gdx.graphics.getHeight() -Gdx.input.getY(),
				Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
	

		
		// set multiplier to full power because a key switch is on or off
		engine.getSystem(PlayerControlSystem.class).movementMultiplier = 1;
		
		//forward
		engine.getSystem(PlayerControlSystem.class).moveForward = Gdx.input.isKeyPressed(Keys.W);
		//right
		engine.getSystem(PlayerControlSystem.class).moveRight	= Gdx.input.isKeyPressed(Keys.D);
		//left
		engine.getSystem(PlayerControlSystem.class).moveLeft    = Gdx.input.isKeyPressed(Keys.A);
		//breaks
		engine.getSystem(PlayerControlSystem.class).applyBreaks = Gdx.input.isKeyPressed(Keys.S);
		
		//DEBUG instant stop
		engine.getSystem(PlayerControlSystem.class).stop = Gdx.input.isKeyJustPressed(Keys.X);
		
		//shoot
		engine.getSystem(PlayerControlSystem.class).shoot = (Gdx.input.isKeyPressed(Keys.SPACE) || Gdx.input.isTouched());
		
		//enter/exit vehicle
		if (Gdx.input.isKeyPressed(Keys.G)) {
			if (engine.getSystem(PlayerControlSystem.class).isInVehicle()) {
				engine.getSystem(PlayerControlSystem.class).exitVehicle();
			} else {
				engine.getSystem(PlayerControlSystem.class).enterVehicle();
			}
		}

	}

}
