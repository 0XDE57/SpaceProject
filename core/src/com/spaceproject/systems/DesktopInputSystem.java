package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class DesktopInputSystem extends EntitySystem implements InputProcessor {
    
    KeyConfig keyCFG;
    private ImmutableArray<Entity> players;

    @Override
    public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
        keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
        
        MyScreenAdapter.getInputMultiplexer().addProcessor(this);
    }
    
    @Override
    public void update(float delta) {
        if (players.size() == 0)
            return;
        Entity player = players.first();
        CameraFocusComponent cameraFocus = player.getComponent(CameraFocusComponent.class);
        if (cameraFocus != null) {
            cameraControls(player, cameraFocus, delta);
        }
    }
    
    private boolean playerControls(int keycode, boolean keyDown) {
        if (players.size() == 0)
            return false;
        
        boolean handled = false;
        
        ControllableComponent control = Mappers.controllable.get(players.first());
        
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
        if (keycode == keyCFG.defend) {
            control.defend = keyDown;
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
        
        if (keycode == keyCFG.actionA) {
            control.actionA = keyDown;
            handled = true;
        }
        if (keycode == keyCFG.actionB) {
            control.actionB = keyDown;
            handled = true;
        }
        if (keycode == keyCFG.actionC) {
            control.actionC = keyDown;
            handled = true;
        }
        
        
        return handled;
    }
    
    private boolean playerFace(int x, int y) {
        if (players.size() == 0)
            return false;
        
        ControllableComponent control = Mappers.controllable.get(players.first());
        
        float angle = MyMath.angleTo(x, Gdx.graphics.getHeight() - y,
                Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        control.angleFacing = angle;
        return true;
    }
    
    private void cameraControls(Entity entity, CameraFocusComponent cameraFocus, float delta) {

        //debug
        if (Gdx.input.isKeyPressed(keyCFG.zoomSpace)) {
            if (MyScreenAdapter.cam.zoom >= 10f) {
                cameraFocus.zoomTarget = 60;
            } else {
                cameraFocus.zoomTarget = 10;
            }
        }
        if (Gdx.input.isKeyPressed(keyCFG.resetZoom)) {
            cameraFocus.zoomTarget = 1;
        }
        if (Gdx.input.isKeyPressed(keyCFG.zoomCharacter)) {
            cameraFocus.zoomTarget = 0.1f;
        }
        if (Gdx.input.isKeyPressed(keyCFG.zoomOut)) {
            cameraFocus.zoomTarget = MyScreenAdapter.cam.zoom + 0.001f;
        }
        if (Gdx.input.isKeyPressed(keyCFG.zoomIn)) {
            cameraFocus.zoomTarget = MyScreenAdapter.cam.zoom - 0.001f;
        }
        if (Gdx.input.isKeyPressed(keyCFG.rotateRight)) {
            MyScreenAdapter.cam.rotate(5f * delta);
        }
        if (Gdx.input.isKeyPressed(keyCFG.rotateLeft)) {
            MyScreenAdapter.cam.rotate(-5f * delta);
        }
    }
    
    @Override
    public boolean scrolled(int amount) {
        if (players.size() != 0) {
            Entity player = players.first();
            CameraFocusComponent cameraFocus = player.getComponent(CameraFocusComponent.class);
            if (cameraFocus != null) {
                float scrollAmount = amount * MyScreenAdapter.cam.zoom / 2;
                cameraFocus.zoomTarget = MyScreenAdapter.cam.zoom += scrollAmount;
            }
        }
        
        return false;
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
        
        Entity player = players.first();
        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            CameraFocusComponent cameraFocus = player.getComponent(CameraFocusComponent.class);
            if (cameraFocus != null) {
                GameScreen.resetCamera();
                EngineConfig engineConfig = SpaceProject.configManager.getConfig(EngineConfig.class);
                if (player.getComponent(VehicleComponent.class) != null) {
                    cameraFocus.zoomTarget = engineConfig.defaultZoomVehicle;
                } else {
                    cameraFocus.zoomTarget = engineConfig.defaultZoomCharacter;
                }
                return true;
            }
        } else {
            ControllableComponent control = Mappers.controllable.get(players.first());
            control.attack = true;
            return true;
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
        return playerFace(screenX, screenY);
    }
    
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return playerFace(screenX, screenY);
    }
    
}
