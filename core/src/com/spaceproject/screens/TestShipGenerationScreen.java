package com.spaceproject.screens;

import java.util.ArrayList;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.RenderingSystem;

public class TestShipGenerationScreen extends ScreenAdapter {

	SpaceProject game;
	
	public Engine engine;
	
	public TestShipGenerationScreen(SpaceProject game) {
		this.game = game;		
		engine = new Engine();
				
		
		//generate test ships
		for (Entity ent : generateShips()) {
			engine.addEntity(ent);
		}
		
		
		engine.addSystem(new RenderingSystem());
		engine.addSystem(new DebugUISystem());
				
	}

	private ArrayList<Entity> generateShips() {
		ArrayList<Entity> ents = new ArrayList<Entity>();
		
		for (int i = 0; i <= 20; ++i) {
			//engine.addEntity(EntityFactory.createShip((110 * i) - Gdx.graphics.getWidth()/2, 200));
			//engine.addEntity(EntityFactory.createShip2((110 * i) - Gdx.graphics.getWidth()/2, 100));
			ents.add(EntityFactory.createShip3((110 * i) - Gdx.graphics.getWidth()/2, 150));
			ents.add(EntityFactory.createShip3((110 * i) - Gdx.graphics.getWidth()/2, 0));
			ents.add(EntityFactory.createShip3((110 * i) - Gdx.graphics.getWidth()/2, -150));
		}
		
		return ents;
	}
	
	public void render(float delta) {
		
		//update engine
		engine.update(delta);
		
			
		if (Gdx.input.isKeyPressed(Keys.COMMA))  engine.getSystem(RenderingSystem.class).zoom(10);	
		if (Gdx.input.isKeyPressed(Keys.PERIOD)) engine.getSystem(RenderingSystem.class).zoom(1);
		if (Gdx.input.isKeyPressed(Keys.SLASH)) engine.getSystem(RenderingSystem.class).zoom(0.1f);
		
		if (Gdx.input.isKeyJustPressed(Keys.R)) {
			//generate new set of ships
			SpaceProject.SEED = MathUtils.random(Long.MAX_VALUE);
			System.out.println(SpaceProject.SEED);
			engine.removeAllEntities();
			for (Entity ent : generateShips()) {
				engine.addEntity(ent);
			}
		}
		
		//terminate------------------------------------------------
		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) Gdx.app.exit();

	}
	
	//resize game
	public void resize(int width, int height) {
		Gdx.app.log("screen", width + ", " + height);
		engine.getSystem(RenderingSystem.class).resize(width, height);
	}

	public void dispose() { }

	public void hide() { }

	public void pause() { }

	public void resume() { }
	
}
