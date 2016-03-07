package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.systems.BoundsSystem;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.systems.MovementSystem;
import com.spaceproject.systems.PlayerControlSystem;
import com.spaceproject.systems.RenderingSystem;
import com.spaceproject.systems.TouchUISystem;

public class WorldScreen extends ScreenAdapter {

	SpaceProject game;

	public static Engine engine;

	public WorldScreen(SpaceProject game) {
		this.game = game;
		// engine to handle all entities and components
		engine = new Engine();
		
		Entity player = EntityFactory.createCharacter(0, 0);
		player.add(new PlayerFocusComponent());
		engine.addEntity(player);
		
		engine.addSystem(new PlayerControlSystem(this, player));
		engine.addSystem(new RenderingSystem());
		engine.addSystem(new MovementSystem());
		engine.addSystem(new CameraSystem());
		engine.addSystem(new DebugUISystem());
		engine.addSystem(new BoundsSystem());
		
		//add input system. touch on android and keys on desktop.
		if (Gdx.app.getType() == ApplicationType.Android) {
			engine.addSystem(new TouchUISystem());
		} else {
			engine.addSystem(new DesktopInputSystem());
		}
	}
	
	
	public void render(float delta) {		
		//update engine
		engine.update(delta);
	}

	
	// resize game
	public void resize(int width, int height) {
		Gdx.app.log("screen", width + ", " + height);
		engine.getSystem(RenderingSystem.class).resize(width, height);
	}

	public void dispose() {
		// TODO: clean up after self
		// dispose of all spritebatches and whatnot
		// create dispose method in all systems and call?

	}

	public void hide() { }

	public void pause() { }

	public void resume() { }
}
