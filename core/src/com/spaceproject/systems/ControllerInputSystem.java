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
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DashComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.utility.Mappers;

public class ControllerInputSystem extends EntitySystem implements ControllerListener {
    
    private float leftStickHorAxis;
    private float leftStickVertAxis;
    private float rightStickHorAxis;
    private float rightStickVertAxis;
    private final float deadZone = 0.25f;
    private final float camZoomSpeed = 3f;
    //private SimpleTimer doubleTap = new SimpleTimer(1000);
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
    public void update(float deltaTime) {
        super.update(deltaTime);
    
        if (Math.abs(rightStickVertAxis) >= deadZone) {
            //Gdx.app.log(this.getClass().getSimpleName(), rightStickVertAxis + " - right vert");
            if (players.size() != 0) {
                Entity player = players.first();
                CameraFocusComponent cameraFocus = Mappers.camFocus.get(player);
                if (cameraFocus != null) {
                    float scrollAmount = rightStickVertAxis * camZoomSpeed * MyScreenAdapter.cam.zoom * deltaTime;
                    cameraFocus.zoomTarget = MyScreenAdapter.cam.zoom += scrollAmount;
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
                shield.defend = buttonDown;
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
            
            /* //todo: double tap for dodge
            if (buttonDown) {
                if (doubleTap.getLastEvent() != 0 && doubleTap.canDoEvent()) {
                    Gdx.app.log("", "double tap unlatch");
                    doubleTap.setLastEvent(0);
                }
                if (doubleTap.getLastEvent() == 0) {
                    Gdx.app.log("", "double tap begin latch");
                    doubleTap.reset();
                } else if (!doubleTap.canDoEvent()) {
                    Gdx.app.log("", "double tap activate");
                    control.alter = true;
                }
            } else {
                control.alter = false;
            }*/
            
            handled = true;
        }
        if (buttonCode == controller.getMapping().buttonL1) {
            control.movementMultiplier = 1;
            control.moveLeft = buttonDown;
            //control.alter = buttonDown;
            handled = true;
        }
        
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
        
        if ((buttonCode == controller.getMapping().buttonRightStick) && buttonDown) {
                //reset cam
                CameraFocusComponent cameraFocus = Mappers.camFocus.get(player);
                if (cameraFocus != null) {
                    GameScreen.resetCamera();
                    EngineConfig engineConfig = SpaceProject.configManager.getConfig(EngineConfig.class);
                    if (Mappers.vehicle.get(player) != null) {
                        cameraFocus.zoomTarget = engineConfig.defaultZoomVehicle;
                    } else {
                        cameraFocus.zoomTarget = engineConfig.defaultZoomCharacter;
                    }
                    handled = true;
                }
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
        //Gdx.app.log(this.getClass().getSimpleName(), controller.getName() + ": " + axisCode + ": " + value);
        
        if (axisCode == controller.getMapping().axisLeftX) {
            leftStickHorAxis = value;
            //Gdx.app.log(this.getClass().getSimpleName(), "left horizontal " + value);
        }
        if (axisCode == controller.getMapping().axisLeftY) {
            leftStickVertAxis = value;
            //Gdx.app.log(this.getClass().getSimpleName(), "left vertical " + value);
        }
    
        ControllableComponent control = Mappers.controllable.get(players.first());
        /*if (axisCode == xboxControllerRightTrigger) {
            control.attack = (value > 0);
        }*/
        
        float dist = Math.abs(MyMath.distance(0, 0, leftStickHorAxis, leftStickVertAxis));
        if (dist >= deadZone) {
            control.angleTargetFace = MyMath.angle2(0, 0, -leftStickVertAxis, leftStickHorAxis);
            control.movementMultiplier = MathUtils.clamp(dist, 0, 1);
            control.moveForward = true;
    
            //notify mouse that controller has current focus
            getEngine().getSystem(DesktopInputSystem.class).controllerHasFocus = true;
            
        } else {
            control.moveForward = false;
        }
        
        
        if (axisCode == controller.getMapping().axisRightX) {
            rightStickHorAxis = value;
        }
        if (axisCode == controller.getMapping().axisRightY) {
            rightStickVertAxis = value;
        }
        
        return false;
    }
    
}
