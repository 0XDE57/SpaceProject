package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AstronomicalComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.ScreenTransitionComponent.LandAnimStage;
import com.spaceproject.components.ScreenTransitionComponent.TakeOffAnimStage;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;
import com.spaceproject.screens.MyScreenAdapter;


public class ScreenTransitionSystem extends IteratingSystem implements EntityListener {

    Family test;
    public ImmutableArray<Entity> astroObjects;

    public ScreenTransitionSystem() {
        super(Family.all(ScreenTransitionComponent.class, TransformComponent.class).get());
    }

    @Override
    public void addedToEngine(Engine engine) {
        test = Family.all(AstronomicalComponent.class, SeedComponent.class).get();
        astroObjects = engine.getEntitiesFor(test);
        engine.addEntityListener(test, this);

        super.addedToEngine(engine);
    }

    @Override
    protected void processEntity(Entity entity, float delta) {
        ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(entity);

        if (screenTrans.landStage != null) {
            if (screenTrans.curLandStage == null || screenTrans.curLandStage != screenTrans.landStage) {
                System.out.println("Animation Stage: " + screenTrans.landStage + " for " + Misc.myToString(entity));
                screenTrans.curLandStage = screenTrans.landStage;
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
                    System.out.println("Animation complete. Removed ScreenTransitionComponent for " + Misc.myToString(entity));
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
                System.out.println(this.getClass().getSimpleName() + ": Animation Stage: " + screenTrans.takeOffStage + " for " + Misc.myToString(entity));
                screenTrans.curTakeOffStage = screenTrans.takeOffStage;
            }
            switch (screenTrans.takeOffStage) {
                case transition:
                    screenTrans.transitioningEntity = entity;
                    if (entity.getComponent(AIComponent.class) != null) {
                        System.out.println("REMOVING AI ENTITY");
                        //getEngine().removeEntity(entity); //TODO: remove entity? backgroundEngine here?
                    } else {
                        takeOff(screenTrans);
                    }

                    break;
                case sync:
                    syncLoadPosition(entity, screenTrans);
                    break;
                case zoomOut:
                    zoomOut(screenTrans);
                    break;
                case grow:
                    grow(entity, screenTrans, delta);
                    break;
                case end:
                    entity.remove(screenTrans.getClass());
                    System.out.println("Animation complete. Removed ScreenTransitionComponent from " + Misc.myToString(entity));
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

        // freeze movement during animation
        TransformComponent transform = Mappers.transform.get(entity);
        if (transform != null) {
            transform.velocity.set(0, 0);
            ControllableComponent control = entity.getComponent(ControllableComponent.class);
            if (control != null) {
                //control.* = false;
                control.angleFacing = Mappers.transform.get(entity).rotation;
            }
        }

    }

    private void syncLoadPosition(Entity entity, ScreenTransitionComponent screenTrans) {
        long desiredSeed = screenTrans.planet.getComponent(SeedComponent.class).seed;
        System.out.println(Misc.myToString(entity) + " is waiting for " + desiredSeed);
        for (Entity astroEnt : astroObjects) {
            //System.out.println(Mappers.seed.get(astroEnt).seed);
            if (Mappers.seed.get(astroEnt).seed == desiredSeed) {
                Vector2 orbitPos = OrbitSystem.getSyncPos(astroEnt, GameScreen.gameTimeCurrent);
                Mappers.transform.get(entity).pos.set(orbitPos);
                screenTrans.takeOffStage = TakeOffAnimStage.zoomOut;
                System.out.println("FOUND SEED "+ desiredSeed);
                break;
            }
        }
    }

    @Override
    public void entityAdded(Entity entity) {
        //System.out.println("Astro object: " + Mappers.seed.get(entity).seed);
        //Misc.printEntity(entity);
    }


    @Override
    public void entityRemoved(Entity entity) {
        //System.out.println("entityRemoved");
        //Misc.printEntity(entity);
    }


    private static void shrink(Entity entity, ScreenTransitionComponent screenTrans, float delta) {
        TextureComponent tex = Mappers.texture.get(entity);

        //shrink texture
        tex.scale -= 3f * delta;
        if (tex.scale <= 0.1f) {
            tex.scale = 0;

            if (entity.getComponent(AIComponent.class) != null) {
                screenTrans.landStage = LandAnimStage.end;
                //getEngine().removeEntity(entity); //TODO: remove entity? backgroundEngine here?
            } else {
                screenTrans.landStage = LandAnimStage.zoomIn;
            }
        }
    }

    private static void grow(Entity entity, ScreenTransitionComponent screenTrans, float delta) {
        TextureComponent tex = Mappers.texture.get(entity);

        //grow texture
        tex.scale += 3f * delta;
        if (tex.scale >= SpaceProject.scale) {
            tex.scale = SpaceProject.scale;

            screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.end;
        }
    }

    private static void zoomIn(ScreenTransitionComponent screenTrans) {
        MyScreenAdapter.setZoomTarget(0);
        if (MyScreenAdapter.cam.zoom <= 0.01f)
            screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.transition;
    }

    private static void zoomOut(ScreenTransitionComponent screenTrans) {
        MyScreenAdapter.setZoomTarget(1);
        if (MyScreenAdapter.cam.zoom == 1)
            screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.grow;
    }


    private static void landOnPlanet(ScreenTransitionComponent screenTrans) {
        screenTrans.landStage = LandAnimStage.pause;
        screenTrans.transitioningEntity.add(screenTrans);
        screenTrans.transitioningEntity.getComponent(TextureComponent.class).scale = SpaceProject.scale;//reset size to normal

        GameScreen.transition = true;
    }

    private void takeOff(ScreenTransitionComponent screenTrans) {
        screenTrans.takeOffStage = TakeOffAnimStage.sync;//TakeOffAnimStage.zoomOut;
        //screenTrans.transitioningEntity.add(screenTrans);
        screenTrans.transitioningEntity.getComponent(TextureComponent.class).scale = 0;//set size to 0 so texture can grow
        screenTrans.planet = GameScreen.currentPlanet; //TODO: genericize so AI can take off, this is a bad singleton


        GameScreen.transition = true;
        //TODO: do current systems run in the background during change?
        //if so, disable/pause and cleanup/dispose
    }

    private static void pause(ScreenTransitionComponent screenTrans, float delta) {
        //TODO use SimpleTimer...
        //TODO move value to config
        int transitionTime = 2200;
        screenTrans.timer += 1000 * delta;
        if (screenTrans.timer >= transitionTime)
            screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.exit;

    }

    private static void exit(Entity entity, ScreenTransitionComponent screenTrans) {
        if (Mappers.vehicle.get(entity) != null) {
            ControllableComponent control = Mappers.controllable.get(entity);
            if (control != null) {
                control.changeVehicle = true;
                screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.end;
            }
        }
    }
}
