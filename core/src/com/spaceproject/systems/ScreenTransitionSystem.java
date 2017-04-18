package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.ScreenAdapter;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.ScreenTransitionComponent.LandAnimStage;
import com.spaceproject.components.ScreenTransitionComponent.TakeOffAnimStage;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.LandConfig;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.screens.SpaceScreen;
import com.spaceproject.screens.WorldScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyScreenAdapter;

public class ScreenTransitionSystem extends IteratingSystem {

	public boolean inSpace;
	
	//TODO: this will not work for multiple entities -> will break when I add AI
	LandAnimStage curLandStage = null;
	TakeOffAnimStage curTakeOffStage = null;
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
		
		

		if (screenTrans.landStage != null) {
			if (curLandStage == null || curLandStage != screenTrans.landStage) {
				System.out.println("Animation Stage: " + screenTrans.landStage);
				curLandStage = screenTrans.landStage;
			}
			switch (screenTrans.landStage) {
			case shrink:
				shrink(entity, screenTrans, delta);
				break;
			case zoomIn:
				zoomIn(screenTrans);
				break;
			case transition:
				landOnPlanet(screenTrans);
				return;
			case pause:
				pause(screenTrans, delta);
				break;
			case exit:
				exit(entity, screenTrans);
				break;
			case end:
				entity.remove(ScreenTransitionComponent.class);
				System.out.println("Animation complete. Removed ScreenTransitionComponent");
				break;
			default:
				try {
					throw new Exception("Uknown Animation Stage: " + screenTrans.landStage);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		} else if (screenTrans.takeOffStage != null) {
			if (curTakeOffStage == null || curTakeOffStage != screenTrans.takeOffStage) {
				System.out.println("Animation Stage: " + screenTrans.takeOffStage);
				curTakeOffStage = screenTrans.takeOffStage;
			}
			switch (screenTrans.takeOffStage) {
			case transition:
				takeOff(screenTrans);
				return;
			case zoomOut:
				zoomOut(screenTrans);
				break;
			case grow:
				grow(entity, screenTrans, delta);
				break;
			case end:
				entity.remove(screenTrans.getClass());
				System.out.println("Animation complete. Removed ScreenTransitionComponent");
				break;
			default:
				try {
					throw new Exception("Uknown Animation Stage: " + screenTrans.takeOffStage);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		}

		// freeze movement during animation
		TransformComponent transform = Mappers.transform.get(entity);
		if (transform != null) {
			transform.velocity.set(0, 0);
			ControllableComponent control = entity.getComponent(ControllableComponent.class);
			if (control != null) {
				control.angleFacing = Mappers.transform.get(entity).rotation;
			}
		}

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
			screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.zoomIn;
		}
	}
	
	private static void grow(Entity entity, ScreenTransitionComponent screenTrans, float delta) {
		TextureComponent tex = Mappers.texture.get(entity);
		
		//grow texture
		tex.scale += 3f * delta; 
		if (tex.scale >= EntityFactory.scale) {
			tex.scale = EntityFactory.scale;
			
			screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.end;
		}
	}

	private static void zoomIn(ScreenTransitionComponent screenTrans) {
		MyScreenAdapter.setZoomTarget(0);
		if (MyScreenAdapter.cam.zoom <= 0.01f) {
			screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.transition;
		}
	}
	
	private static void zoomOut(ScreenTransitionComponent screenTrans) {
		MyScreenAdapter.setZoomTarget(1);
		if (MyScreenAdapter.cam.zoom == 1) {
			screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.grow;
		}
	}
	
	private static void landOnPlanet(ScreenTransitionComponent screenTrans) {
		screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.pause;
		screenTrans.landCFG.ship.add(screenTrans);
		screenTrans.landCFG.ship.getComponent(TextureComponent.class).scale = 4;//reset size to normal	
		
		MyScreenAdapter.changeScreen(new WorldScreen(screenTrans.landCFG));		
	}
	
	private void takeOff(ScreenTransitionComponent screenTrans) {
		screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.zoomOut;
		screenTrans.landCFG.ship.add(screenTrans);
		screenTrans.landCFG.ship.getComponent(TextureComponent.class).scale = 0;//set size to 0 so texture can grow
		screenTrans.landCFG.position = landCFG.position;
		
		SpaceScreen x = new SpaceScreen(screenTrans.landCFG);
		x.cam.zoom = 0;
		MyScreenAdapter.changeScreen(x);
		//TODO: do current systems run in the background during change?
	}
		
	private static void pause(ScreenTransitionComponent screenTrans, float delta) {				
		int transitionTime = 5000;
		screenTrans.timer += 1000 * delta;		
		if (screenTrans.timer >= transitionTime) {			
			screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.exit;
		}
	}

	private static void exit(Entity entity, ScreenTransitionComponent screenTrans) {
		if (entity.getComponent(VehicleComponent.class) != null) {
			ControllableComponent control = entity.getComponent(ControllableComponent.class);
			if (control != null) {
				control.changeVehicle = true;
				screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.end;				
			}			
		}
	}
}
