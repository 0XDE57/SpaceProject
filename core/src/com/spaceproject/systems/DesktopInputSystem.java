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
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DashComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class DesktopInputSystem extends EntitySystem implements InputProcessor {
    
    private final KeyConfig keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
    private ImmutableArray<Entity> players;
    private final Vector3 tempVec = new Vector3();
    public boolean controllerHasFocus = false;
    
    private final long doubleTapTime = 300;
    private final SimpleTimer doubleTapLeft = new SimpleTimer(doubleTapTime);
    private final SimpleTimer doubleTapRight = new SimpleTimer(doubleTapTime);
    private int tapCounterLeft = 0;
    private int tapCounterRight = 0;
    
    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
        GameScreen.getInputMultiplexer().addProcessor(this);
    }
    
    @Override
    public void removedFromEngine(Engine engine) {
        GameScreen.getInputMultiplexer().removeProcessor(this);
    }
    
    @Override
    public void update(float delta) {
        if (!controllerHasFocus) {
            facePosition(Gdx.input.getX(), Gdx.input.getY());
        }
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
    
            //check double tap
            if (!keyDown) {
                tapCounterRight++;
                if (tapCounterRight == 1) {
                    //single tap
                    doubleTapRight.reset();
                } else {
                    //double tap
                    tapCounterRight = 0;
                    BarrelRollComponent barrelRoll = Mappers.barrelRoll.get(player);
                    if (barrelRoll != null) {
                        BarrelRollSystem.dodgeRight(player, barrelRoll);
                    }
                }
            }
            //timeout
            if (doubleTapRight.canDoEvent()) {
                tapCounterRight = 0;
            }
            
            handled = true;
        }
        if (keycode == keyCFG.left) {
            control.moveLeft = keyDown;
    
            //check double tap
            if (!keyDown) {
                tapCounterLeft++;
                if (tapCounterLeft == 1) {
                    //single tap
                    doubleTapLeft.reset();
                } else {
                    //double tap
                    tapCounterLeft = 0;
                    BarrelRollComponent barrelRoll = Mappers.barrelRoll.get(player);
                    if (barrelRoll != null) {
                        BarrelRollSystem.dodgeLeft(player, barrelRoll);
                    }
                }
            }
            //timeout
            if (doubleTapLeft.canDoEvent()) {
                tapCounterLeft = 0;
            }
            
            handled = true;
        }
        if (keycode == keyCFG.back) {
            control.moveBack = keyDown;
            
            if (control.moveBack) {
                //cancel hyperdrive if active
                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            handled = true;
        }
        
        if (keycode == keyCFG.boost) {
            control.boost = keyDown;
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
                shield.activate = keyDown;
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
    
    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (amountY <= 0) {
            getEngine().getSystem(CameraSystem.class).zoomIn();
        } else {
            getEngine().getSystem(CameraSystem.class).zoomOut();
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
    
        //primary attack
        if (button == Input.Buttons.LEFT) {
            ControllableComponent control = Mappers.controllable.get(players.first());
            control.attack = true;
            return true;
        }
        
        //reset cam
        if (button == Input.Buttons.MIDDLE) {
            GameScreen.resetRotation();
            getEngine().getSystem(CameraSystem.class).setZoomToDefault(players.first());
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
        return facePosition(screenX, screenY);
    }
    
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        controllerHasFocus = false;
        
        return false;
    }
    
}
