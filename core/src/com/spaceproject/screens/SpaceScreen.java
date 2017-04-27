package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
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
import com.spaceproject.systems.OrbitSystem;
import com.spaceproject.systems.ScreenTransitionSystem;
import com.spaceproject.systems.SpaceLoadingSystem;
import com.spaceproject.systems.SpaceParallaxSystem;
import com.spaceproject.systems.SpaceRenderingSystem;
import com.spaceproject.systems.MobileInputSystem;
import com.spaceproject.utility.MyScreenAdapter;

public class SpaceScreen extends MyScreenAdapter {
	
	public static Engine engine;
	
	public SpaceScreen(LandConfig landCFG) {
		
		// engine to handle all entities and components
		engine = new Engine();
		

		//===============ENTITIES===============
		//test ships
		engine.addEntity(EntityFactory.createShip3(-100, 400));
		engine.addEntity(EntityFactory.createShip3(-200, 400));		
		engine.addEntity(EntityFactory.createShip3(-300, 400));
		engine.addEntity(EntityFactory.createShip3(-400, 400));		

		Entity aiTest = EntityFactory.createCharacter(0, 400);
		aiTest.add(new AIComponent());
		aiTest.add(new ControllableComponent());
		engine.addEntity(aiTest);
		
		//add player
		Entity ship = landCFG.ship;		
		ship.getComponent(TransformComponent.class).pos.x = landCFG.position.x;
		ship.getComponent(TransformComponent.class).pos.y = landCFG.position.y;
		engine.addEntity(ship);
		
		
		//===============SYSTEMS===============
		//input
		if (Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.iOS) {
			engine.addSystem(new MobileInputSystem());
		} else {
			engine.addSystem(new DesktopInputSystem());
		}
		engine.addSystem(new AISystem());
		
		//loading
		engine.addSystem(new SpaceLoadingSystem());
		engine.addSystem(new SpaceParallaxSystem());
		//Ai...
		
		//logic
		engine.addSystem(new ControlSystem(this));
		engine.addSystem(new ExpireSystem(1));
		engine.addSystem(new OrbitSystem());
		engine.addSystem(new MovementSystem());
		engine.addSystem(new BoundsSystem());
		engine.addSystem(new CollisionSystem());
		engine.addSystem(new ScreenTransitionSystem(this));	
		
		//rendering
		engine.addSystem(new CameraSystem());
		engine.addSystem(new SpaceRenderingSystem());
		engine.addSystem(new HUDSystem());
		engine.addSystem(new DebugUISystem());
				
	}
	
	@Override
	public void render(float delta) {
		super.render(delta);
		
		//update engine
		engine.update(delta);
			
		//terminate
		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) Gdx.app.exit();			

	}

	@Override
	public void dispose() {
		System.out.println("Disposing: " + this.getClass().getSimpleName());
		super.dispose();
		
		//clean up after self			
		for (EntitySystem sys : engine.getSystems()) {
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}
		
		//dispose of spritebatches and textures	
		for (Entity ents : engine.getEntitiesFor(Family.all(TextureComponent.class).get())) {
			TextureComponent tex = ents.getComponent(TextureComponent.class);
			if (tex != null)
				tex.texture.dispose();
		}
		
		//engine.removeAllEntities();
		/*
		 * EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x000007fefe7911cb,
		 * pid=5620, tid=5876 # # JRE version: Java(TM) SE Runtime Environment
		 * (8.0_91-b14) (build 1.8.0_91-b14) # Java VM: Java HotSpot(TM) 64-Bit
		 * Server VM (25.91-b14 mixed mode windows-amd64 compressed oops) #
		 * Problematic frame: # C [msvcrt.dll+0x11cb]
		 */
	}
	
	@Override
	public void hide() {
		//dispose();
		/*
		for (EntitySystem sys : engine.getSystems()) {
			if (sys instanceof Disposable)
				((Disposable) sys).dispose();
		}
		
		for (Entity ents : engine.getEntitiesFor(Family.all(TextureComponent.class).get())) {
			TextureComponent tex = ents.getComponent(TextureComponent.class);
			if (tex != null)
				tex.texture.dispose();
		}
		
		engine.removeAllEntities();
		engine = null;*/
	}
	
	@Override
	public void pause() { }
	@Override
	public void resume() { }

}
