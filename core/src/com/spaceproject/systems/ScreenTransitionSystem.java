package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.ScreenAdapter;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.LandConfig;
import com.spaceproject.screens.SpaceScreen;
import com.spaceproject.screens.WorldScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyScreenAdapter;

public class ScreenTransitionSystem extends IteratingSystem {

	public boolean inSpace;	
	//LandConfig landCFG;
	
	public ScreenTransitionSystem(ScreenAdapter screen, LandConfig landConfig) {
		super(Family.all(ScreenTransitionComponent.class, TransformComponent.class).get());
		inSpace = (screen instanceof SpaceScreen);
		
		//this.landCFG = landConfig;
	}

	@Override
	protected void processEntity(Entity entity, float delta) {
		ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(entity); 
		switch (screenTrans.stage) {
		case shrink: shrink(entity, screenTrans, delta); break;
		case zoom: zoom(screenTrans); break;
		case transition: changeScreens(screenTrans); break;
		case pause: pause(screenTrans, delta); break;
		case exit: exit(entity); break;
		default: 
			System.out.println("Uknown Stage: " + screenTrans.stage);
			entity.remove(ScreenTransitionComponent.class);
			System.out.println("Removed ScreenTransitionComponent.class");
			break;
		}
		
		//freeze movement during animation
		entity.getComponent(TransformComponent.class).velocity.set(0, 0);
		
	}

	private static void pause(ScreenTransitionComponent screenTrans, float delta) {
		int transitionTime = 60;
		screenTrans.timer += 10 * delta;
		if (screenTrans.timer >= transitionTime) {
			screenTrans.stage = ScreenTransitionComponent.AnimStage.exit;
			System.out.println("Animation Stage:" + screenTrans.stage);
		}
	}

	private static void exit(Entity entity) {
		
		entity.getComponent(TextureComponent.class).scale = 4;//reset size to normal
		if (entity.getComponent(VehicleComponent.class) != null) {
			ControllableComponent control = entity.getComponent(ControllableComponent.class);
			if (control != null) {
				control.changeVehicle = true;
				entity.remove(ScreenTransitionComponent.class);
				System.out.println("Animation complete. Removed ScreenTransitionComponent");
			}			
		}
		//else entity.remove(ScreenTransitionComponent.class);
	}

	private static void stopShip(Entity entity, ScreenTransitionComponent screenTrans) {
		//TODO: slow before stop instead of instant stop		
		
		screenTrans.stage = ScreenTransitionComponent.AnimStage.shrink;
		
		System.out.println("Animation Stage:" + screenTrans.stage);
	}

	private static void shrink(Entity entity, ScreenTransitionComponent screenTrans, float delta) {
		TextureComponent tex = Mappers.texture.get(entity);
		
		//shrink texture
		tex.scale -= 3f * delta; 
		if (tex.scale <= 0.1f) {
			tex.scale = 0;
			
			//if (enitity is ai)
			//stage = transition;
			//else, next stage
			screenTrans.stage = ScreenTransitionComponent.AnimStage.zoom;
			
			System.out.println("Animation Stage:" + screenTrans.stage);
		}
	}

	private static void zoom(ScreenTransitionComponent screenTrans) {
		MyScreenAdapter.setZoomTarget(0);
		if (MyScreenAdapter.cam.zoom <= 0.1f) {
			screenTrans.stage = ScreenTransitionComponent.AnimStage.transition;	
			System.out.println("Animation Stage:" + screenTrans.stage);
		}
	}
	
	private void changeScreens(ScreenTransitionComponent screenTrans) {
		screenTrans.stage = ScreenTransitionComponent.AnimStage.pause;
		screenTrans.landCFG.ship.add(screenTrans);
		System.out.println("Animation Stage:" + screenTrans.stage);
		MyScreenAdapter.changeScreen(inSpace ? new WorldScreen(screenTrans.landCFG) : new SpaceScreen(screenTrans.landCFG));		
	}
}
