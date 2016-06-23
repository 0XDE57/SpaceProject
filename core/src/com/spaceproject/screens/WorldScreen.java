package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.config.LandConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.systems.BoundsSystem;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.systems.MovementSystem;
import com.spaceproject.systems.PlayerControlSystem;
import com.spaceproject.systems.TouchUISystem;
import com.spaceproject.systems.WorldRenderingSystem;
import com.spaceproject.utility.MyScreenAdapter;

public class WorldScreen extends MyScreenAdapter {

	SpaceProject game;

	public static Engine engine;

	public WorldScreen(SpaceProject game, LandConfig landCFG) {
		this.game = game;
		
		cam.zoom = 1;
		setZoomTarget(1);
		
		// engine to handle all entities and components
		engine = new Engine();
		
		int position = landCFG.planet.mapSize*32/2;//32 = tileSize, set position to middle of planet
		Entity player = EntityFactory.createCharacter(position, position);
		player.add(new CameraFocusComponent());
		engine.addEntity(player);
		
		engine.addSystem(new PlayerControlSystem(this, player, landCFG));
		engine.addSystem(new WorldRenderingSystem(landCFG.planet));
		engine.addSystem(new MovementSystem());
		engine.addSystem(new CameraSystem(cam));
		engine.addSystem(new DebugUISystem(cam, batch, shape));
		engine.addSystem(new BoundsSystem());
		
		//add input system. touch on android and keys on desktop.
		if (Gdx.app.getType() == ApplicationType.Android) {
			engine.addSystem(new TouchUISystem());
		} else {
			engine.addSystem(new DesktopInputSystem());
		}
	}
	
	@Override
	public void render(float delta) {
		super.render(delta);
		
		//update engine
		engine.update(delta);
	}
	

	public void changeScreen(LandConfig landCFG) {
		System.out.println("Change screen to Space.");
		
		//dispose();
		
		game.setScreen(new SpaceScreen(game, landCFG));
	}

	public void dispose() {
		//clean up after self
		//dispose of spritebatches and textures
		for (EntitySystem sys : engine.getSystems()) {			
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}
		
		for (Entity ents : engine.getEntities()) {
			TextureComponent tex = ents.getComponent(TextureComponent.class);
			if (tex != null)
				tex.texture.dispose();
		}
		
		//engine.removeAllEntities();
		
	}

	public void hide() { }

	public void pause() { }

	public void resume() { }
}
