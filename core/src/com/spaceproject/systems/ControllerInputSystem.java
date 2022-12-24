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
import com.spaceproject.components.CannonComponent;
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
    private float l2, r2;
    private final float stickDeadzone = 0.5f;
    private final float triggerDeadZone = 0.1f;
    
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
    }
    
    @Override
    public void connected(Controller controller) {
        logController(controller, true);
    }
    
    @Override
    public void disconnected(Controller controller) {
        logController(controller, false);
    }
    
    private void logController(Controller controller, boolean connected) {
        //todo: bug in jampad?
        // canVibrate() <-- null when disconnecting controller
        /* Exception in thread "main" java.lang.NullPointerException
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.canVibrate(JamepadController.java:173)
	        at com.spaceproject.systems.ControllerInputSystem.logController(ControllerInputSystem.java:73)
	        at com.spaceproject.systems.ControllerInputSystem.disconnected(ControllerInputSystem.java:69)
	        at com.badlogic.gdx.controllers.desktop.support.CompositeControllerListener.disconnected(CompositeControllerListener.java:21)
	        at com.badlogic.gdx.controllers.desktop.support.CompositeControllerListener.disconnected(CompositeControllerListener.java:21)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.setDisconnected(JamepadController.java:90)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.getButton(JamepadController.java:55)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.updateButtonsState(JamepadController.java:137)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.update(JamepadController.java:105)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadControllerMonitor.update(JamepadControllerMonitor.java:52)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadControllerMonitor.run(JamepadControllerMonitor.java:26)
	        at com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.loop(Lwjgl3Application.java:208)
	        at com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.<init>(Lwjgl3Application.java:166)
	        at com.spaceproject.desktop.DesktopLauncher.main(DesktopLauncher.java:16)
        * */
        
        boolean canVibrate = false;
        if (connected) {
            canVibrate = controller.canVibrate();
        }
        
        //if (canVibrate == null)
        String info = (connected ? "Connected: '" : "Disconnected: '")
                + controller.getName()
                + "' id:[" + controller.getUniqueId()
                + "] index:" + controller.getPlayerIndex()
                + " power:" + controller.getPowerLevel()
                + " vibrate:" + canVibrate;
        Gdx.app.log(this.getClass().getSimpleName(), info);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
    
        if (Math.abs(rightStickVertAxis) >= stickDeadzone) {
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
            control.boost = buttonDown;
            
            if (control.boost) {
                control.movementMultiplier = 1;
            }
            
            DashComponent dash = Mappers.dash.get(player);
            if (dash != null) {
                dash.activate = buttonDown;
            }
            handled = true;
        }
        
        if (buttonCode == controller.getMapping().buttonB) {
            HyperDriveComponent hyperDrive = Mappers.hyper.get(player);
            if (hyperDrive != null) {
                hyperDrive.activate = buttonDown;
                handled = true;
            }
        }
        
        if (buttonCode == controller.getMapping().buttonY) {
            control.changeVehicle = buttonDown;
            handled = true;
        }
        
        if (buttonCode == controller.getMapping().buttonX) {
            control.attack = buttonDown;
            handled = true;
        }
    
        if (buttonCode == controller.getMapping().buttonDpadRight) {
            //todo: swap ship weapons
            //burst cannon <-> charge cannon
            //remove one, install the other
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
            CameraSystem camera = getEngine().getSystem(CameraSystem.class);
            if (camera.getZoomLevel() == 2) {
                camera.zoomOutMax();
            } else {
                camera.setZoomToDefault(player);
            }
            handled = true;
        }
        
        return handled;
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
        if (players.size() == 0) return false;
        
        //Gdx.app.log("control:", axisCode + ", " + value);
        //controller.getMapping().buttonL2 = ?
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
        if (axisCode == 4 /*controller.getMapping().buttonL2*/) {
            l2 = value;
        }
        if (axisCode == 5 /* controller.getMapping().buttonR2*/) {
            r2 = value;
        }
        
        Entity player = players.first();
        ControllableComponent control = Mappers.controllable.get(player);
        if (r2 > triggerDeadZone) {
            control.attack = true;
            
            CannonComponent cannon = Mappers.cannon.get(player);
            if (cannon != null) {
                //todo, move this into canon system, just pass on multiplier to control component
                int baseRate = 300;
                int minRate = 40;
                long rateOfFire = (long) (baseRate * (1 - r2));
                if (rateOfFire < minRate) {
                    rateOfFire = minRate;
                }
                cannon.timerFireRate.setInterval(rateOfFire, false);
            }
        } else {
            //todo: bug, stick drift and minor inputs seem to be interfering with mouse input
            control.attack = false;
        }
    
        ShieldComponent shield = Mappers.shield.get(player);
        if (shield != null) {
            if (l2 > triggerDeadZone) {
                shield.activate = true;
            } else {
                shield.activate = false;
            }
        }
        
        float dist = Math.abs(MyMath.distance(0, 0, leftStickHorAxis, leftStickVertAxis));
        if (dist >= stickDeadzone) {
            //face stick direction
            control.angleTargetFace = MyMath.angle2(0, 0, -leftStickVertAxis, leftStickHorAxis);
            control.movementMultiplier = MathUtils.clamp(dist, 0, 1);
            control.moveForward = true;
    
            //notify mouse that controller has current focus
            getEngine().getSystem(DesktopInputSystem.class).controllerHasFocus = true;
        } else {
            if (!control.boost) {
                control.moveForward = false;
            }
        }

        return false;
    }
    
}
