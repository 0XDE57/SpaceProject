package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AstronomicalComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Misc;


public class ScreenTransitionSystem extends IteratingSystem {

    private ImmutableArray<Entity> astroObjects;

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
                case transition:
                    landOnPlanet(entity, screenTrans);
                    return;
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
                Gdx.app.log(this.getClass().getSimpleName(), ": Animation Stage: " + screenTrans.takeOffStage + " for " + Misc.objString(entity));
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

    private static void shrink(Entity entity, ScreenTransitionComponent screenTrans) {
        /*
        TextureComponent tex = Mappers.texture.get(entity);
        if (tex != null) {

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
        */

        // freeze movement during animation
        TransformComponent transform = Mappers.transform.get(entity);
        if (transform != null) {
            transform.velocity.set(0, 0);
        }



        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        float interp = screenTrans.animInterpolation.apply(1, 0, screenTrans.timer.ratio());
        sprite3D.renderable.scale.set(interp, interp, interp);
        //TODO: something is wrong with scaling/rendering. the interpolation numbers feel right, but often the entity often much larger than it should be
        //also the entity scale should be down to 0 by the end but the sprite sometimes only partially scales or seemingly doesn't scale at all.
        /*
        TransformComponent trans = Mappers.transform.get(entity);
        String text = MyMath.round(sprite3D.renderable.scale.x, 3) + ", " + MyMath.round(sprite3D.renderable.scale.y, 3);
        DebugUISystem.addTempText(text, trans.pos.x, trans.pos.y, true);
        //System.out.println(text);
        */


        if (screenTrans.timer.tryEvent()) {
            sprite3D.renderable.scale.set(0, 0, 0);
            if (entity.getComponent(AIComponent.class) != null) {
                screenTrans.doTransition = true;
            } else {
                screenTrans.landStage = screenTrans.landStage.next();
            }
        }
    }

    private static void grow(Entity entity, ScreenTransitionComponent screenTrans) {
        /*
        TextureComponent tex = Mappers.texture.get(entity);
        if (tex != null) {
            //grow texture
            tex.scale += 3f * delta;
            if (tex.scale >= SpaceProject.entitycfg.renderScale) {
                tex.scale = SpaceProject.entitycfg.renderScale;

                screenTrans.takeOffStage = screenTrans.takeOffStage.next();
            }
        }*/

        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);

        float interp = screenTrans.animInterpolation.apply(0, 1, screenTrans.timer.ratio());
        sprite3D.renderable.scale.set(interp, interp, interp);

        if (screenTrans.timer.canDoEvent()) {
            sprite3D.renderable.scale.set(1, 1, 1);
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


    private static void landOnPlanet(Entity entity, ScreenTransitionComponent screenTrans) {
        /*
        //reset size to normal
        TextureComponent tex = Mappers.texture.get(entity);
        if (tex != null) {
            tex.scale = SpaceProject.entitycfg.renderScale;
        }*/

        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        sprite3D.renderable.scale.set(1,1,1);

        screenTrans.doTransition = true;
    }

    private void takeOff(Entity entity, ScreenTransitionComponent screenTrans) {
        //set size to 0 so texture can grow
        /*
        TextureComponent tex = Mappers.texture.get(entity);
        if (tex != null) {
            tex.scale = 0;
        }*/

        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        sprite3D.renderable.scale.set(0,0,0);

        screenTrans.doTransition = true;
    }

    private void syncLoadPosition(Entity entity, ScreenTransitionComponent screenTrans) {
        long desiredSeed = screenTrans.planet.getComponent(SeedComponent.class).seed;
        Gdx.app.log(this.getClass().getSimpleName(), Misc.objString(entity) + " is waiting for " + desiredSeed);
        for (Entity astroEnt : astroObjects) {
            if (Mappers.seed.get(astroEnt).seed == desiredSeed) {
                Vector2 orbitPos = OrbitSystem.getSyncPos(astroEnt, GameScreen.gameTimeCurrent);
                Mappers.transform.get(entity).pos.set(orbitPos);
                screenTrans.takeOffStage = screenTrans.takeOffStage.next();
                Gdx.app.log(this.getClass().getSimpleName(), "FOUND SEED "+ desiredSeed);
                break;
            }
        }
    }

    private static void pause(ScreenTransitionComponent screenTrans) {
        if (screenTrans.timer.tryEvent()) {
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
