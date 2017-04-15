package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.ScreenAdapter;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.ScreenTransitionComponent.AnimStage;
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
	
	//TODO: this will not work for multiple entities -> will break when I add AI
	AnimStage curStage = null;
	LandConfig landCFG;
	
	public ScreenTransitionSystem(ScreenAdapter screen) {
		super(Family.all(ScreenTransitionComponent.class, TransformComponent.class).get());
		inSpace = (screen instanceof SpaceScreen);
	}

	public ScreenTransitionSystem(WorldScreen worldScreen, LandConfig landConfig) {
		this(worldScreen);
		landCFG = landConfig;
	}

	@Override
	protected void processEntity(Entity entity, float delta) {
		ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(entity); 
		
		
		if (curStage == null || curStage != screenTrans.stage) {
			System.out.println("Animation Stage: " + screenTrans.stage);
			curStage = screenTrans.stage;
		}
				
		switch (screenTrans.stage) {
			case shrink: shrink(entity, screenTrans, delta); break;
			case zoom: zoom(screenTrans); break;
			case transition: changeScreens(screenTrans); break;
			case pause: pause(screenTrans, delta); break;
			case exit: exit(entity); break;
		default:
			try {
				throw new Exception("Uknown Animation Stage: " + screenTrans.stage);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		
		//freeze movement during animation
		Mappers.transform.get(entity).velocity.set(0, 0);
		Mappers.controllable.get(entity).angleFacing = Mappers.transform.get(entity).rotation;
		
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
		}
	}

	private static void zoom(ScreenTransitionComponent screenTrans) {
		MyScreenAdapter.setZoomTarget(0);
		if (MyScreenAdapter.cam.zoom <= 0.01f) {
			screenTrans.stage = ScreenTransitionComponent.AnimStage.transition;
		}
	}
	
	private void changeScreens(ScreenTransitionComponent screenTrans) {
		screenTrans.stage = ScreenTransitionComponent.AnimStage.pause;
		screenTrans.landCFG.ship.add(screenTrans);
		screenTrans.landCFG.ship.getComponent(TextureComponent.class).scale = 4;//reset size to normal
		
		if (!inSpace) {
			screenTrans.landCFG.position = landCFG.position;
		}
		
		MyScreenAdapter.changeScreen(inSpace ? new WorldScreen(screenTrans.landCFG) : new SpaceScreen(screenTrans.landCFG));		
	}
		
	private static void pause(ScreenTransitionComponent screenTrans, float delta) {				
		int transitionTime = 5000;
		screenTrans.timer += 1000 * delta;		
		if (screenTrans.timer >= transitionTime) {			
			screenTrans.stage = ScreenTransitionComponent.AnimStage.exit;
		}
	}

	private static void exit(Entity entity) {
		if (entity.getComponent(VehicleComponent.class) != null) {
			ControllableComponent control = entity.getComponent(ControllableComponent.class);
			if (control != null) {
				control.changeVehicle = true;
				entity.remove(ScreenTransitionComponent.class);
				System.out.println("Animation complete. Removed ScreenTransitionComponent");
			}			
		}
	}
}
