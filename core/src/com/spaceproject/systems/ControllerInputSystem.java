package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DashComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class ControllerInputSystem extends EntitySystem implements ControllerListener {
    
    private float leftStickHorAxis;
    private float leftStickVertAxis;
    private float rightStickHorAxis;
    private float rightStickVertAxis;
    private final float deadZone = 0.25f;
    
    private final SimpleTimer cameraDelayTimer = new SimpleTimer(400);
    
    private final long doubleTapTime = 300;
    private final SimpleTimer doubleTapLeft = new SimpleTimer(doubleTapTime);
    private final SimpleTimer doubleTapRight = new SimpleTimer(doubleTapTime);
    private int tapCounterLeft = 0;
    private int tapCounterRight = 0;
    
    private ImmutableArray<Entity> players;
    
    @Override
    public void addedToEngine(Engine engine) {
        Controllers.addListener(this);
        
        for (Controller controller : Controllers.getControllers()) {
            Gdx.app.log(this.getClass().getSimpleName(), controller.getName());
        }
        
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    public void removedFromEngine(Engine engine) {
        Controllers.removeListener(this);
        Gdx.app.debug(this.getClass().getSimpleName(), "removed controller listener");
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
    
        if (Math.abs(rightStickVertAxis) >= deadZone) {
            if (cameraDelayTimer.tryEvent()) {
                if (rightStickVertAxis >= 0) {
                    getEngine().getSystem(CameraSystem.class).zoomOut();
                } else {
                    getEngine().getSystem(CameraSystem.class).zoomIn();
                }
            }
        }
    }
    
    private boolean playerControls(Controller controller, int buttonCode, boolean buttonDown) {
        if (players.size() == 0)
            return false;
        
        boolean handled = false;
        
        Entity player = players.first();
        ControllableComponent control = Mappers.controllable.get(player);
        
        if (buttonCode == controller.getMapping().buttonA) {
            control.attack = buttonDown;
            handled = true;
            
            DashComponent dash = Mappers.dash.get(player);
            if (dash != null) {
                dash.activate = buttonDown;
                handled = true;
            }
        }
        
        if (buttonCode == controller.getMapping().buttonB) {
            ShieldComponent shield = Mappers.shield.get(player);
            if (shield != null) {
                shield.activate = buttonDown;
                handled = true;
            }
        }
        
        if (buttonCode == controller.getMapping().buttonY) {
            control.changeVehicle = buttonDown;
            handled = true;
        }
        
        if (buttonCode == controller.getMapping().buttonX) {
            control.alter = buttonDown;
            handled = true;
        }
    
        if (buttonCode == controller.getMapping().buttonDpadUp) {
            HyperDriveComponent hyperDrive = Mappers.hyper.get(player);
            if (hyperDrive != null) {
                hyperDrive.activate = buttonDown;
                handled = true;
            }
        }
        
        if (buttonCode == controller.getMapping().buttonDpadDown) {
            control.transition = buttonDown;
            handled = true;
        }
        
        if (buttonCode == controller.getMapping().buttonR1) {
            control.movementMultiplier = 1;
            control.moveRight = buttonDown;
            
            //check double tap
            if (!buttonDown) {
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
        
        if (buttonCode == controller.getMapping().buttonL1) {
            control.movementMultiplier = 1;
            control.moveLeft = buttonDown;
    
            //check double tap
            if (!buttonDown) {
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
        
        //toggle menu
        if (buttonCode == controller.getMapping().buttonStart) {
            if (buttonDown) {
                GameMenu menu = getEngine().getSystem(HUDSystem.class).getGameMenu();
                if (!menu.isVisible()) {
                    menu.show();
                } else {
                    menu.close();
                }
            }
            handled = true;
        }
        
        //reset cam
        if ((buttonCode == controller.getMapping().buttonRightStick) && buttonDown) {
            GameScreen.resetRotation();
            getEngine().getSystem(CameraSystem.class).setZoomToDefault(player);
            handled = true;
        }
        
        return handled;
    }
    
    @Override
    public void connected(Controller controller) {
        Gdx.app.log(this.getClass().getSimpleName(), "Connected: " + controller.getName());
    }
    
    @Override
    public void disconnected(Controller controller) {
        Gdx.app.log(this.getClass().getSimpleName(), "Disconnected: " + controller.getName());
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        return playerControls(controller, buttonCode, true);
    }
    
    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return playerControls(controller, buttonCode, false);
    }
    
    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (axisCode == controller.getMapping().axisLeftX) {
            leftStickHorAxis = value;
        }
        if (axisCode == controller.getMapping().axisLeftY) {
            leftStickVertAxis = value;
        }
        if (axisCode == controller.getMapping().axisRightX) {
            rightStickHorAxis = value;
        }
        if (axisCode == controller.getMapping().axisRightY) {
            rightStickVertAxis = value;
        }
        
        ControllableComponent control = Mappers.controllable.get(players.first());
        float dist = Math.abs(MyMath.distance(0, 0, leftStickHorAxis, leftStickVertAxis));
        if (dist >= deadZone) {
            //face stick direction
            control.angleTargetFace = MyMath.angle2(0, 0, -leftStickVertAxis, leftStickHorAxis);
            control.movementMultiplier = MathUtils.clamp(dist, 0, 1);
            control.moveForward = true;
    
            //notify mouse that controller has current focus
            getEngine().getSystem(DesktopInputSystem.class).controllerHasFocus = true;
        } else {
            control.moveForward = false;
        }

        return false;
    }
    
}
