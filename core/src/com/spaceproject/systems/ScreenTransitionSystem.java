package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AstronomicalComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.State;
import com.spaceproject.ui.TransitionOverlay;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;


public class ScreenTransitionSystem extends IteratingSystem implements IRequireGameContext {
    
    private GameScreen gameScreen;
    
    public ScreenTransitionSystem() {
        super(Family.all(ScreenTransitionComponent.class, TransformComponent.class).get());
    }
    
    @Override
    public void initContext(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(entity);
        
        if (screenTrans.landStage != null) {
            if (screenTrans.curLandStage == null || screenTrans.curLandStage != screenTrans.landStage) {
                Gdx.app.log(this.getClass().getSimpleName(), "Animation Stage: " + screenTrans.landStage + " for " + Misc.objString(entity));
                screenTrans.curLandStage = screenTrans.landStage;
            }
            switch (screenTrans.landStage) {
                case shrink:
                    shrink(entity, screenTrans);
                    break;
                case zoomIn:
                    zoomIn(screenTrans);
                    break;
                case screenEffectFadeIn:
                    fadeIn(screenTrans);
                    break;
                case transition:
                    landOnPlanet(gameScreen, entity, screenTrans);
                    return;
                case screenEffectFadeOut:
                    fadeOut(screenTrans);
                    break;
                case pause:
                    pause(screenTrans);
                    break;
                case exit:
                    exit(entity, screenTrans);
                    break;
                case end:
                    entity.remove(ScreenTransitionComponent.class);
                    Gdx.app.log(this.getClass().getSimpleName(), "Animation complete. Removed ScreenTransitionComponent for " + Misc.objString(entity));
                    break;
                default:
                    try {
                        throw new Exception("Unknown Animation Stage: " + screenTrans.landStage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else if (screenTrans.takeOffStage != null) {
            if (screenTrans.curTakeOffStage == null || screenTrans.curTakeOffStage != screenTrans.takeOffStage) {
                Gdx.app.log(this.getClass().getSimpleName(), "Animation Stage: " + screenTrans.takeOffStage + " for " + Misc.objString(entity));
                screenTrans.curTakeOffStage = screenTrans.takeOffStage;
            }
            switch (screenTrans.takeOffStage) {
                case screenEffectFadeIn:
                    fadeIn(screenTrans);
                    break;
                case transition:
                    takeOff(gameScreen, entity, screenTrans);
                    break;
                case sync:
                    syncLoadPosition(entity, screenTrans);
                    break;
                case screenEffectFadeOut:
                    fadeOut(screenTrans);
                    break;
                case zoomOut:
                    zoomOut(screenTrans);
                    break;
                case grow:
                    grow(entity, screenTrans);
                    break;
                case end:
                    entity.remove(screenTrans.getClass());
                    Gdx.app.log(this.getClass().getSimpleName(), "Animation complete. Removed ScreenTransitionComponent from " + Misc.objString(entity));
                    break;
                default:
                    try {
                        throw new Exception("Unknown Animation Stage: " + screenTrans.takeOffStage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }
    
    public static void nextStage(ScreenTransitionComponent screenTrans) {
        if (screenTrans.landStage != null) {
            screenTrans.landStage = screenTrans.landStage.next();
        }
        if (screenTrans.takeOffStage != null) {
            screenTrans.takeOffStage = screenTrans.takeOffStage.next();
        }
    }
    
    private static void shrink(Entity entity, ScreenTransitionComponent screenTrans) {
        
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            // freeze movement during animation
            physics.body.setLinearVelocity(0, 0);
            
            if (screenTrans.rotation == 0.0f) {
                screenTrans.rotation = MathUtils.random(0.01f, -0.01f);
            }
            physics.body.setTransform(physics.body.getPosition(), physics.body.getAngle() + screenTrans.rotation);
        }
        
        
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        if (screenTrans.initialScale == 0) {
            screenTrans.initialScale = sprite3D.renderable.scale.x;
        }
        float interp = screenTrans.animInterpolation.apply(screenTrans.initialScale, 0, screenTrans.timer.ratio());
        sprite3D.renderable.scale.set(interp, interp, interp);
        
        if (screenTrans.timer.tryEvent()) {
            sprite3D.renderable.scale.set(0, 0, 0);
            if (entity.getComponent(AIComponent.class) != null) {
                screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.transition;
            } else {
                nextStage(screenTrans);
            }
        }
    }
    
    private static void grow(Entity entity, ScreenTransitionComponent screenTrans) {
    
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            //match planet vel
            //transform.velocity.set(screenTrans.planet.getComponent(TransformComponent.class).velocity);
            
            if (screenTrans.rotation == 0.0f) {
                screenTrans.rotation = MathUtils.random(0.01f, -0.01f);
            }
            physics.body.setTransform(physics.body.getPosition(), physics.body.getAngle() + screenTrans.rotation);
        }
        
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        float interp = screenTrans.animInterpolation.apply(0, screenTrans.initialScale, screenTrans.timer.ratio());
        sprite3D.renderable.scale.set(interp, interp, interp);
        
        if (screenTrans.timer.canDoEvent()) {
            sprite3D.renderable.scale.set(screenTrans.initialScale, screenTrans.initialScale, screenTrans.initialScale);
            nextStage(screenTrans);
        }
    }
    
    private void zoomIn(ScreenTransitionComponent screenTrans) {
        MyScreenAdapter.setZoomTarget(0.05f);
        if (MyScreenAdapter.cam.zoom <= 0.05f) {
            nextStage(screenTrans);
        }
    
        //begin fade while also zoom
        //TODO: would probably look better to sync camera zoom with fade amount
        HUDSystem hud = getEngine().getSystem(HUDSystem.class);
        TransitionOverlay overlay = hud.getTransitionOverlay();
        if (overlay.getFadeState() == State.off) {
            overlay.fadeIn();
        }
    }
    
    private static void zoomOut(ScreenTransitionComponent screenTrans) {
        MyScreenAdapter.setZoomTarget(1);
        if (MyScreenAdapter.cam.zoom >= 1) {
            screenTrans.timer.reset();
            nextStage(screenTrans);
        }
    }
    
    private void fadeIn(ScreenTransitionComponent screenTrans) {
        HUDSystem hud = getEngine().getSystem(HUDSystem.class);
        TransitionOverlay overlay = hud.getTransitionOverlay();
        
        switch (overlay.getFadeState()) {
            case off:
                overlay.fadeIn();
                break;
            case on:
                nextStage(screenTrans);
                break;
        }
        
    }
    
    private void fadeOut(ScreenTransitionComponent screenTrans) {
        HUDSystem hud = getEngine().getSystem(HUDSystem.class);
        TransitionOverlay overlay = hud.getTransitionOverlay();
        
        switch (overlay.getFadeState()) {
            case off:
                nextStage(screenTrans);
                break;
            case on:
                overlay.fadeOut();
                break;
        }
    }
    
    private static void landOnPlanet(GameScreen gameContext, Entity entity, ScreenTransitionComponent screenTrans) {
        //reset size to normal
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        sprite3D.renderable.scale.set(screenTrans.initialScale, screenTrans.initialScale, screenTrans.initialScale);
        
        gameContext.switchScreen(entity, screenTrans.planet);
    }
    
    private void takeOff(GameScreen gameContext, Entity entity, ScreenTransitionComponent screenTrans) {
        //set size to 0 so texture can grow
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        sprite3D.renderable.scale.set(0, 0, 0);
    
        MyScreenAdapter.setZoomTarget(0);
        MyScreenAdapter.cam.zoom = 0;
        
        gameContext.switchScreen(entity, null);
    }
    
    private void syncLoadPosition(Entity entity, ScreenTransitionComponent screenTrans) {
        long desiredSeed = screenTrans.planet.getComponent(SeedComponent.class).seed;
        Gdx.app.log(this.getClass().getSimpleName(), Misc.objString(entity) + " is waiting for " + desiredSeed);
        
        Family astro = Family.all(AstronomicalComponent.class, SeedComponent.class).get();
        ImmutableArray<Entity> astroObjects = getEngine().getEntitiesFor(astro);
        for (Entity astroEnt : astroObjects) {
            if (Mappers.seed.get(astroEnt).seed == desiredSeed) {
                Vector2 orbitPos = OrbitSystem.getSyncPos(astroEnt, GameScreen.getGameTimeCurrent());
                Mappers.transform.get(entity).pos.set(orbitPos);
                nextStage(screenTrans);
                Gdx.app.log(this.getClass().getSimpleName(), "FOUND SEED " + desiredSeed);
                break;
            }
        }
    }
    
    private static void pause(ScreenTransitionComponent screenTrans) {
        if (screenTrans.timer.tryEvent()) {
            nextStage(screenTrans);
        }
    }
    
    private static void exit(Entity entity, ScreenTransitionComponent screenTrans) {
        if (Mappers.vehicle.get(entity) != null) {
            ControllableComponent control = Mappers.controllable.get(entity);
            if (control != null) {
                control.changeVehicle = true;
                nextStage(screenTrans);
            }
        }
    }
    
}
