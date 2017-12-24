package com.spaceproject.screens;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.LandConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.systems.AISystem;
import com.spaceproject.systems.BoundsSystem;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.CollisionSystem;
import com.spaceproject.systems.DebugUISystem;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.systems.ExpireSystem;
import com.spaceproject.systems.HUDSystem;
import com.spaceproject.systems.MovementSystem;
import com.spaceproject.systems.ControlSystem;
import com.spaceproject.systems.ScreenTransitionSystem;
import com.spaceproject.systems.MobileInputSystem;
import com.spaceproject.systems.WorldRenderingSystem;
import com.spaceproject.systems.WorldWrapSystem;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyScreenAdapter;

@Deprecated
public class WorldScreen extends MyScreenAdapter {

	public static Engine engine;	

	public WorldScreen(LandConfig landCFG) {
		System.out.println("WorldScreen()");
		
		// engine to handle all entities and components
		engine = new Engine();
		
		//===============ENTITIES===============				
		//add player
		Entity ship = landCFG.ship;
		int position = landCFG.planet.mapSize*32/2;//32 = tileSize, set position to middle of planet
		ship.getComponent(TransformComponent.class).pos.x = position;
		ship.getComponent(TransformComponent.class).pos.y = position;
		engine.addEntity(ship);
		
		//test ships near player
		engine.addEntity(EntityFactory.createShip3(position+100, position+300));
		engine.addEntity(EntityFactory.createShip3(position-100, position+300));	
		
		Entity aiTest = EntityFactory.createCharacter(position, position+50);
		aiTest.add(new AIComponent());
		aiTest.add(new ControllableComponent());
		engine.addEntity(aiTest);
		//engine.addEntity(Misc.copyEntity(aiTest));
		//engine.addEntity(Misc.copyEntity(aiTest));
		//engine.addEntity(Misc.copyEntity(aiTest));
		//engine.addEntity(Misc.copyEntity(aiTest));
		
		
		//===============SYSTEMS===============
		//input
		if (Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.iOS) {
			engine.addSystem(new MobileInputSystem());
		} else {
			engine.addSystem(new DesktopInputSystem());
		}
		engine.addSystem(new AISystem());
		
		
		//loading
		
		
		//logic
		//engine.addSystem(new ScreenTransitionSystem(this, landCFG));
		engine.addSystem(new ControlSystem(this));
		//engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new MovementSystem());
		engine.addSystem(new WorldWrapSystem(landCFG.planet.mapSize));
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new CollisionSystem());
		
		
		//rendering
		engine.addSystem(new CameraSystem());
		engine.addSystem(new WorldRenderingSystem(landCFG.planet));
		engine.addSystem(new HUDSystem());
		engine.addSystem(new DebugUISystem());		
		
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
		
		super.dispose();
		
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
