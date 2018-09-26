package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AstronomicalComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;


public class ScreenTransitionSystem extends IteratingSystem {

    public ImmutableArray<Entity> astroObjects;

    public ScreenTransitionSystem() {
        super(Family.all(ScreenTransitionComponent.class, TransformComponent.class).get());
    }

    @Override
    public void addedToEngine(Engine engine) {
        Family astro = Family.all(AstronomicalComponent.class, SeedComponent.class).get();
        astroObjects = engine.getEntitiesFor(astro);

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
                    landOnPlanet(entity, screenTrans);

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
                    takeOff(entity, screenTrans);
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

    private static void shrink(Entity entity, ScreenTransitionComponent screenTrans, float delta) {
        TextureComponent tex = Mappers.texture.get(entity);

        //shrink texture
        tex.scale -= 3f * delta;
        if (tex.scale <= 0.1f) {
            tex.scale = 0;

            if (entity.getComponent(AIComponent.class) != null) {
                screenTrans.doTransition = true;
            } else {
                screenTrans.landStage = screenTrans.landStage.next();
            }
        }
    }

    private static void grow(Entity entity, ScreenTransitionComponent screenTrans, float delta) {
        TextureComponent tex = Mappers.texture.get(entity);

        //grow texture
        tex.scale += 3f * delta;
        if (tex.scale >= SpaceProject.scale) {
            tex.scale = SpaceProject.scale;

            screenTrans.takeOffStage = screenTrans.takeOffStage.next();
        }
    }

    private static void zoomIn(ScreenTransitionComponent screenTrans) {
        MyScreenAdapter.setZoomTarget(0);
        if (MyScreenAdapter.cam.zoom <= 0.01f) {
            screenTrans.landStage = screenTrans.landStage.next();
        }
    }

    private static void zoomOut(ScreenTransitionComponent screenTrans) {
        MyScreenAdapter.setZoomTarget(1);
        if (MyScreenAdapter.cam.zoom == 1) {
            screenTrans.takeOffStage = screenTrans.takeOffStage.next();
        }
    }


    private static void landOnPlanet(Entity transitioningEntity, ScreenTransitionComponent screenTrans) {
        transitioningEntity.getComponent(TextureComponent.class).scale = SpaceProject.scale;//reset size to normal
        screenTrans.doTransition = true;

    }

    private void takeOff(Entity transitioningEntity, ScreenTransitionComponent screenTrans) {
        transitioningEntity.getComponent(TextureComponent.class).scale = 0;//set size to 0 so texture can grow
        screenTrans.doTransition = true;
    }

    private void syncLoadPosition(Entity entity, ScreenTransitionComponent screenTrans) {
        long desiredSeed = screenTrans.planet.getComponent(SeedComponent.class).seed;
        System.out.println(Misc.myToString(entity) + " is waiting for " + desiredSeed);
        for (Entity astroEnt : astroObjects) {
            if (Mappers.seed.get(astroEnt).seed == desiredSeed) {
                Vector2 orbitPos = OrbitSystem.getSyncPos(astroEnt, GameScreen.gameTimeCurrent);
                Mappers.transform.get(entity).pos.set(orbitPos);
                screenTrans.takeOffStage = screenTrans.takeOffStage.next();
                System.out.println("FOUND SEED "+ desiredSeed);
                break;
            }
        }
    }

    private static void pause(ScreenTransitionComponent screenTrans, float delta) {
        //TODO use SimpleTimer...
        //TODO move value to config
        int transitionTime = 2200;
        screenTrans.timer += 1000 * delta;
        if (screenTrans.timer >= transitionTime) {
            screenTrans.landStage = screenTrans.landStage.next();
        }

    }

    private static void exit(Entity entity, ScreenTransitionComponent screenTrans) {
        if (Mappers.vehicle.get(entity) != null) {
            ControllableComponent control = Mappers.controllable.get(entity);
            if (control != null) {
                control.changeVehicle = true;
                screenTrans.landStage = screenTrans.landStage.next();
            }
        }
    }
}
