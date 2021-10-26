package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DashComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;

public class DesktopInputSystem extends EntitySystem implements InputProcessor {
    
    private final KeyConfig keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
    private ImmutableArray<Entity> players;
    private final Vector3 tempVec = new Vector3();
    public boolean controllerHasFocus = false;
    
    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
        MyScreenAdapter.getInputMultiplexer().addProcessor(this);
    }
    
    @Override
    public void update(float delta) {
        if (!controllerHasFocus) {
            facePosition(Gdx.input.getX(), Gdx.input.getY());
        }
        
        debugCameraControls(delta);
    }
    
    private boolean playerControls(int keycode, boolean keyDown) {
        if (players.size() == 0)
            return false;
        
        boolean handled = false;
    
        Entity player = players.first();
        ControllableComponent control = Mappers.controllable.get(player);
        
        //movement
        control.movementMultiplier = 1; // set multiplier to full power because a key switch is on or off
        if (keycode == keyCFG.forward) {
            control.moveForward = keyDown;
            handled = true;
        }
        if (keycode == keyCFG.right) {
            control.moveRight = keyDown;
            handled = true;
        }
        if (keycode == keyCFG.left) {
            control.moveLeft = keyDown;
            handled = true;
        }
        if (keycode == keyCFG.back) {
            control.moveBack = keyDown;
            handled = true;
        }
        
        if (keycode == keyCFG.alter) {
            control.alter = keyDown;
            handled = true;
        }
        
        if (keycode == keyCFG.changeVehicle) {
            control.changeVehicle = keyDown;
            handled = true;
        }
        if (keycode == keyCFG.land) {
            control.transition = keyDown;
            handled = true;
        }
    
        if (keycode == keyCFG.dash) {
            DashComponent dash = Mappers.dash.get(player);
            if (dash != null) {
                dash.activate = keyDown;
                handled = true;
            }
        }
        
        if (keycode == keyCFG.activateShield) {
            ShieldComponent shield = Mappers.shield.get(player);
            if (shield != null) {
                shield.defend = keyDown;
                handled = true;
            }
        }
        if (keycode == keyCFG.activateHyperDrive) {
            HyperDriveComponent hyperDrive = Mappers.hyper.get(player);
            if (hyperDrive != null) {
                hyperDrive.activate = keyDown;
                handled = true;
            }
        }
        
        return handled;
    }
    
    private boolean facePosition(int x, int y) {
        if (players.size() == 0)
            return false;
    
        TransformComponent transform = Mappers.transform.get(players.first());
        ControllableComponent control = Mappers.controllable.get(players.first());
        
        Vector3 playerPos = GameScreen.cam.project(tempVec.set(transform.pos, 0));
        float angle = MyMath.angleTo(x, Gdx.graphics.getHeight() - y, playerPos.x, playerPos.y);
        control.angleTargetFace = angle;
        
        return true;
    }
    
    private void debugCameraControls(float delta) {
        if (players.size() == 0) {
            return;
        }
        
        Entity player = players.first();
        CameraFocusComponent cameraFocus = Mappers.camFocus.get(player);
        if (cameraFocus == null) {
            return;
        }
        
        float zoomSpeed = 2f * delta;
        float angle = 5f * delta;
        
        if (Gdx.input.isKeyPressed(keyCFG.resetZoom)) {
            cameraFocus.zoomTarget = 1;
        }
        if (Gdx.input.isKeyPressed(keyCFG.zoomOut)) {
            cameraFocus.zoomTarget = MyScreenAdapter.cam.zoom + zoomSpeed;
        }
        if (Gdx.input.isKeyPressed(keyCFG.zoomIn)) {
            cameraFocus.zoomTarget = MyScreenAdapter.cam.zoom - zoomSpeed;
        }
        if (Gdx.input.isKeyPressed(keyCFG.rotateRight)) {
            MyScreenAdapter.cam.rotate(angle);
        }
        if (Gdx.input.isKeyPressed(keyCFG.rotateLeft)) {
            MyScreenAdapter.cam.rotate(-angle);
        }
    }
    
    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (players.size() != 0) {
            Entity player = players.first();
            CameraFocusComponent cameraFocus = Mappers.camFocus.get(player);
            if (cameraFocus != null) {
                float scrollAmount = amountY * cameraFocus.zoomTarget * 0.5f;
                // a += Math.round(b * a * 0.5f);
                // go nicely between 0.25, 0.5, 1, 2, 3, 5, 8, 12, 18, 27... but not quite 13, 21?
                // it turns out to be strangely close to fib unintentionally,
                // but also feels like a really nice spacing between zooms
                // iter:    0,   1, 2, 3, 4, 5, 6,  7, 8...
                // fib:     0,   1, 1, 2, 3, 5, 8, 13, 21..
                // out:  0.25, 0.5, 1, 2, 3, 5, 8, 13, 21..
                //todo: do it intentionally with fib, move it to camera class so controller input and mobile input set same targets
                //for (int i = 0; i < 10; i++ ) { Gdx.app.debug("iter: " + i, MyMath.fibonacci(i) + ""); }
                //if (amountY > 0) { iter++; } else { iter--; }
                //cameraFocus.zoomTarget = getZoom(iter);
                
                if (amountY <= 0) {
                    //zoom in
                    if (cameraFocus.zoomTarget == 0.5f) {
                        cameraFocus.zoomTarget = 0.25f;
                    } else if (cameraFocus.zoomTarget == 1f) {
                        cameraFocus.zoomTarget = 0.5f;
                    } else {
                        cameraFocus.zoomTarget += Math.round(scrollAmount);
                    }
                } else {
                    //zoom out
                    if (cameraFocus.zoomTarget == 0.25f) {
                        cameraFocus.zoomTarget = 0.5f;
                    } else if (cameraFocus.zoomTarget == 0.5f) {
                        cameraFocus.zoomTarget = 1f;
                    } else {
                        cameraFocus.zoomTarget += Math.max(1, Math.round(scrollAmount));
                    }
                }
            }
        }
        
        return false;
    }
    
    static byte iter = 0;//126;test out of bound and negative
    private static float getZoom(byte iter) {
        switch (iter) {
            case 0: return 0.25f;
            case 1: return 0.5f; //default character zoom
            case 2: return 1.0f; //default vehicle zoom
            default: return MyMath.fibonacci(iter);
        }
    }
    
    @Override
    public boolean keyDown(int keycode) {
        return playerControls(keycode, true);
    }
    
    @Override
    public boolean keyUp(int keycode) {
        return playerControls(keycode, false);
    }
    
    @Override
    public boolean keyTyped(char character) {
        return false;
    }
    
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (players.size() == 0) {
            return false;
        }
    
        if (button == Input.Buttons.LEFT) {
            ControllableComponent control = Mappers.controllable.get(players.first());
            control.attack = true;
            return true;
        }
        
        if (button == Input.Buttons.MIDDLE) {
            Entity player = players.first();
            CameraFocusComponent cameraFocus = Mappers.camFocus.get(player);
            if (cameraFocus != null) {
                GameScreen.resetRotation();
                EngineConfig engineConfig = SpaceProject.configManager.getConfig(EngineConfig.class);
                if (Mappers.vehicle.get(player) != null) {
                    cameraFocus.zoomTarget = engineConfig.defaultZoomVehicle;
                } else {
                    cameraFocus.zoomTarget = engineConfig.defaultZoomCharacter;
                }
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (players.size() != 0) {
            ControllableComponent control = Mappers.controllable.get(players.first());
            control.attack = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return facePosition(screenX, screenY);
    }
    
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        controllerHasFocus = false;
        
        return false;
    }
    
}
