package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class DesktopInputSystem extends EntitySystem implements InputProcessor, IRequireGameContext {
    
    KeyConfig keyCFG;
    private ImmutableArray<Entity> players;
    
    @Override
    public void initContext(GameScreen gameScreen) {
        gameScreen.getInputMultiplexer().addProcessor(this);
    }
    
    @Override
    public void addedToEngine(com.badlogic.ashley.core.Engine engine) {
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
        keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
    }
    
    
    @Override
    public void update(float delta) {
        cameraControls(delta);
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
    
    
    private void cameraControls(float delta) {
        //zoom test
        if (Gdx.input.isKeyPressed(keyCFG.zoomSpace)) {
            if (MyScreenAdapter.cam.zoom >= 10f) {
                MyScreenAdapter.setZoomTarget(60);
            } else {
                MyScreenAdapter.setZoomTarget(10);
            }
        }
        if (Gdx.input.isKeyPressed(keyCFG.resetZoom)) {
            MyScreenAdapter.setZoomTarget(1);
        }
        if (Gdx.input.isKeyPressed(keyCFG.zoomCharacter)) {
            MyScreenAdapter.setZoomTarget(0.1f);
        }
        if (Gdx.input.isKeyPressed(keyCFG.zoomOut)) {
            MyScreenAdapter.setZoomTarget(MyScreenAdapter.cam.zoom + 0.001f);
        }
        if (Gdx.input.isKeyPressed(keyCFG.zoomIn)) {
            MyScreenAdapter.setZoomTarget(MyScreenAdapter.cam.zoom - 0.001f);
        }
        if (Gdx.input.isKeyPressed(keyCFG.rotateRight)) {
            MyScreenAdapter.cam.rotate(5f * delta);
        }
        if (Gdx.input.isKeyPressed(keyCFG.rotateLeft)) {
            MyScreenAdapter.cam.rotate(-5f * delta);
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
        if (players.size() != 0) {
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
    
    @Override
    public boolean scrolled(int amount) {
        return false;
    }
    
    
}
