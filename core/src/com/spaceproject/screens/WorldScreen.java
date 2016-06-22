package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PlayerFocusComponent;
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

public class WorldScreen extends ScreenAdapter {

	SpaceProject game;

	public static Engine engine;
	
	private static OrthographicCamera cam;
	private static SpriteBatch batch;
	private static ShapeRenderer shape;
	
	//LandConfig landCFG;

	public WorldScreen(SpaceProject game, LandConfig landCFG) {
		this.game = game;
		
		cam = new OrthographicCamera();
		batch = new SpriteBatch();
		shape = new ShapeRenderer();
		
		//this.landCFG = landCFG;
		
		// engine to handle all entities and components
		engine = new Engine();
		
		int position = landCFG.planet.mapSize*32/2;//32 = tileSize, set position to middle of planet
		Entity player = EntityFactory.createCharacter(position, position);
		player.add(new PlayerFocusComponent());
		engine.addEntity(player);
		
		engine.addSystem(new PlayerControlSystem(this, player, landCFG));
		engine.addSystem(new WorldRenderingSystem(landCFG.planet, cam));
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
	
	
	public void render(float delta) {		
		//update engine
		engine.update(delta);
	}
	
	
	// resize game
	public void resize(int width, int height) {
		Gdx.app.log("screen", width + ", " + height);
		engine.getSystem(WorldRenderingSystem.class).resize(width, height);
	}

	public void changeScreen(LandConfig landCFG) {
		System.out.println("Change screen to Space.");
		
		//dispose();
		
		game.setScreen(new SpaceScreen(game, landCFG));
	}

	public void dispose() {
		//clean up after self
		//dispose of spritebatches and textures
		//engine.
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
