package com.spaceproject.screens;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.LandConfig;
import com.spaceproject.systems.BoundsSystem;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.systems.MovementSystem;
import com.spaceproject.systems.NewControlSystem;
import com.spaceproject.systems.ScreenTransitionSystem;
import com.spaceproject.systems.TouchUISystem;
import com.spaceproject.systems.WorldRenderingSystem;
import com.spaceproject.utility.MyScreenAdapter;

public class WorldScreen extends MyScreenAdapter {

	public static Engine engine;	

	public WorldScreen(LandConfig landCFG) {

		// engine to handle all entities and components
		engine = new Engine();
		
	
		//add player
		Entity ship = landCFG.ship;
		int position = landCFG.planet.mapSize*32/2;//32 = tileSize, set position to middle of planet
		ship.getComponent(TransformComponent.class).pos.x = position;
		ship.getComponent(TransformComponent.class).pos.y = position;
		engine.addEntity(ship);


		
		//engine.addSystem(new PlayerControlSystem(this, player, landCFG));
		engine.addSystem(new ScreenTransitionSystem(this, landCFG));
		engine.addSystem(new NewControlSystem(this, engine));
		
		engine.addSystem(new WorldRenderingSystem(landCFG.planet));
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
	
	@Override
	public void render(float delta) {
		super.render(delta);
		
		//update engine
		engine.update(delta);
	}
	
	@Override
	public void dispose() {
		System.out.println("Disposing: " + this.getClass().getSimpleName());
		
		//clean up after self
		//dispose of spritebatches and textures
		for (EntitySystem sys : engine.getSystems()) {			
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}
		
		for (Entity ents : engine.getEntitiesFor(Family.all(TextureComponent.class).get())) {
			TextureComponent tex = ents.getComponent(TextureComponent.class);
			if (tex != null)
				tex.texture.dispose();
		}
		
		//engine.removeAllEntities();
		
	}
	
	@Override
	public void hide() {
		//dispose();
	}
	
	@Override
	public void pause() { }
	@Override
	public void resume() { }
}
