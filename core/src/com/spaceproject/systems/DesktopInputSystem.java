package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.spaceproject.EntityFactory;

public class DesktopInputSystem extends EntitySystem {
	
	private Engine engine;
	
	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;

	}
	
	Entity[] temp;
	
	@Override
	public void update(float delta) {
		
		//[DEBUG]//////////////////////////////		
		if (Gdx.input.isKeyJustPressed(Keys.K)) {
			if (temp != null) {
				//remove-----------------
				for (Entity entity : temp) {
					engine.removeEntity(entity);
				}
				temp = null;
				System.out.println("removed");
			} else {
				//add------------------
				temp = EntityFactory.createPlanetarySystem(0, 0);
				for (Entity entity : temp) {
					engine.addEntity(entity);
				}
				System.out.println("added");
			}
		}
		//[DEGUB]/////////////////////////////

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
	
		//move forward
		if (Gdx.input.isKeyPressed(Keys.W)) {
			engine.getSystem(PlayerControlSystem.class).move = true;
			// set multiplier to full power because a key switch is on or off
			engine.getSystem(PlayerControlSystem.class).movementMultiplier = 1; 
		} else {
			engine.getSystem(PlayerControlSystem.class).move = false;
		}
		
		//stop ship
		if (Gdx.input.isKeyJustPressed(Keys.S)) {
			engine.getSystem(PlayerControlSystem.class).stop = true;
		}
		
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
