package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.spaceproject.EntityFactory;
import com.spaceproject.SpaceProject;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.RenderingSystem;

public class TestShipGenerationScreen extends ScreenAdapter {

	SpaceProject game;
	
	public Engine engine;
	
	public TestShipGenerationScreen(SpaceProject game) {
		this.game = game;
		
		engine = new Engine();
		
		
		//generate test ships
		for (int i = 0; i <= 20; ++i) {
			//engine.addEntity(EntityFactory.createShip((110 * i) - Gdx.graphics.getWidth()/2, 200));
			//engine.addEntity(EntityFactory.createShip2((110 * i) - Gdx.graphics.getWidth()/2, 100));

			engine.addEntity(EntityFactory.createShip3((110 * i) - Gdx.graphics.getWidth()/2, 150));
			engine.addEntity(EntityFactory.createShip3((110 * i) - Gdx.graphics.getWidth()/2, 0));
			engine.addEntity(EntityFactory.createShip3((110 * i) - Gdx.graphics.getWidth()/2, -150));
		}
		
		
		//engine.addSystem(new );
		engine.addSystem(new RenderingSystem());
		engine.addSystem(new DebugUISystem());
		
		
	}
	
	public void render(float delta) {
		
		//update engine
		engine.update(delta);
		
			
		if (Gdx.input.isKeyPressed(Keys.COMMA))  engine.getSystem(RenderingSystem.class).zoom(10);	
		if (Gdx.input.isKeyPressed(Keys.PERIOD)) engine.getSystem(RenderingSystem.class).zoom(1);
		if (Gdx.input.isKeyPressed(Keys.SLASH)) engine.getSystem(RenderingSystem.class).zoom(0.1f);
		
		
		
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
