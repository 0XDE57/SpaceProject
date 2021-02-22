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
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.controllers.mappings.Xbox;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.utility.Mappers;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.SimpleTimer;

public class ControllerInputSystem extends EntitySystem implements ControllerListener {
    
    private ImmutableArray<Entity> players;
    
    public ControllerInputSystem() {
        Controllers.addListener(this);
        
        for (Controller controller : Controllers.getControllers()) {
            Gdx.app.log(this.getClass().getSimpleName(), controller.getName());
        }
    }
    
    private SimpleTimer doubleTap = new SimpleTimer(1000);
    private boolean playerControls(int buttonCode, boolean buttonDown) {
        Gdx.app.log(this.getClass().getSimpleName(), "button: " + buttonCode + ": " + buttonDown);
        
        if (players.size() == 0)
            return false;
        
        boolean handled = false;
        
        ControllableComponent control = Mappers.controllable.get(players.first());
    
        if (buttonCode == Xbox.A) {
            control.attack = buttonDown;
            handled = true;
        }
        if (buttonCode == Xbox.B) {
            ShieldComponent shield = Mappers.shield.get(players.first());
            if (shield != null) {
                shield.defend = buttonDown;
                handled = true;
            }
        }
        if (buttonCode == Xbox.Y) {
            control.changeVehicle = buttonDown;
            handled = true;
        }
        if (buttonCode == Xbox.X) {
            control.alter = buttonDown;
            handled = true;
        }
    
        if (buttonCode == Xbox.DPAD_UP) {
            HyperDriveComponent hyperDrive = Mappers.hyper.get(players.first());
            if (hyperDrive != null) {
                hyperDrive.activate = buttonDown;
                handled = true;
            }
        }
        if (buttonCode == Xbox.DPAD_DOWN) {
            control.transition = buttonDown;
            handled = true;
        }
        
        if (buttonCode == Xbox.R_BUMPER) {
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
        if (buttonCode == Xbox.L_BUMPER) {
            control.movementMultiplier = 1;
            control.moveLeft = buttonDown;
            //control.alter = buttonDown;
            handled = true;
        }
        
        if (buttonCode == Xbox.START) {
            GameMenu menu = getEngine().getSystem(HUDSystem.class).getGameMenu();
            if (buttonDown) {
                if (!menu.isVisible()) {
                    menu.show();
                } else {
                    menu.close();
                }
            }
            
            handled = true;
        }
        
        if (buttonCode == Xbox.R_STICK) {
            //reset cam
            Entity player = players.first();
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
        }
        
        
        return handled;
    }
    
    
    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
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
        return playerControls(buttonCode, true);
    }
    
    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return playerControls(buttonCode, false);
    }
    
    
    float leftStickHorAxis;
    float leftStickVertAxis;
    float rightStickHorAxis;
    float rightStickVertAxis;
    float dist;
    
    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        //Gdx.app.log(this.getClass().getSimpleName(), controller.getName() + ":" + axisCode + ": " + value);
        float deadZone = 0.25f;
        //controller.getMapping()

        if (axisCode == Xbox.L_STICK_HORIZONTAL_AXIS) {
            //if (value >= deadZone) {
                leftStickHorAxis = value;
            //}
            Gdx.app.log(this.getClass().getSimpleName(), "left horizontal " + value);
        }
        if (axisCode == Xbox.L_STICK_VERTICAL_AXIS) {
            //if (value >= deadZone) {
                leftStickVertAxis = value;
            //}
            Gdx.app.log(this.getClass().getSimpleName(), "left vertical " + value);
        }
        
        //Gdx.app.log(this.getClass().getSimpleName(), "d " + dist);
        
    
        dist = MyMath.distance(0, 0, leftStickHorAxis, leftStickVertAxis);
        ControllableComponent control = Mappers.controllable.get(players.first());
        if (dist >= deadZone) {
            Gdx.app.log(this.getClass().getSimpleName(), controller.getName() + " left stick > deadZone: " + axisCode + ": " + value);
            control.angleTargetFace = MyMath.angle2(0, 0, -leftStickVertAxis, leftStickHorAxis);
            control.movementMultiplier = MathUtils.clamp(dist, 0, 1);
            control.moveForward = true;
        } else {
            control.moveForward = false;
        }
        
        
        if (axisCode == Xbox.R_STICK_HORIZONTAL_AXIS) {
            rightStickHorAxis = value;
        }
        if (axisCode == Xbox.R_STICK_VERTICAL_AXIS) {
            rightStickVertAxis = value;
            if (rightStickVertAxis >= deadZone) {
                Gdx.app.log(this.getClass().getSimpleName(), rightStickVertAxis + " - right vert");
    
                if (players.size() != 0) {
                    Entity player = players.first();
                    CameraFocusComponent cameraFocus = player.getComponent(CameraFocusComponent.class);
                    if (cameraFocus != null) {
                        float scrollAmount = rightStickHorAxis * MyScreenAdapter.cam.zoom / 2;
                        //cameraFocus.zoomTarget = MyScreenAdapter.cam.zoom += scrollAmount;
                    }
                }
            }
        }

        return false;
    }
    
    @Override
    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
        //Gdx.app.log(this.getClass().getSimpleName(), povCode + " " + value);
        return false;
    }
    
    @Override
    public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
        //Gdx.app.log(this.getClass().getSimpleName(), sliderCode + " " + value);
        return false;
    }
    
    @Override
    public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
        //Gdx.app.log(this.getClass().getSimpleName(), sliderCode + " " + value);
        return false;
    }
    
    @Override
    public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
        //Gdx.app.log(this.getClass().getSimpleName(), accelerometerCode + " " + value);
        return false;
    }
    
}
