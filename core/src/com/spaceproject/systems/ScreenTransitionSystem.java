package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.ScreenAdapter;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
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
		case stopShip: stopShip(entity, screenTrans); break;
		case shrink: shrink(entity, screenTrans, delta); break;
		case zoom: zoom(screenTrans); break;
		case transition: changeScreens(screenTrans); break;
		default: System.out.println("Uknown Stage: " + screenTrans.stage); break;
		}
		
	}

	private static void stopShip(Entity entity, ScreenTransitionComponent screenTrans) {
		//TODO: slow before stop instead of instant stop		
		entity.getComponent(TransformComponent.class).velocity.set(0, 0);
		screenTrans.stage = ScreenTransitionComponent.AnimStage.shrink;
		
		System.out.println("stop -> shrink");
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
			
			System.out.println("shrink -> zoom");
		}
	}

	private static void zoom(ScreenTransitionComponent screenTrans) {
		MyScreenAdapter.setZoomTarget(0);
		if (MyScreenAdapter.cam.zoom <= 0.1f) {
			screenTrans.stage = ScreenTransitionComponent.AnimStage.transition;	
			System.out.println("zoom -> transition");
		}
	}
	
	private void changeScreens(ScreenTransitionComponent screen) {
		MyScreenAdapter.changeScreen(inSpace ? new WorldScreen(screen.landCFG) : new SpaceScreen(screen.landCFG));		
	}
}
