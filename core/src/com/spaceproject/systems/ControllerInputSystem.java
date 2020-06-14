package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

public class ControllerInputSystem extends EntitySystem implements ControllerListener {

    public ControllerInputSystem() {
        Controllers.addListener(this);
        
        for (Controller controller : Controllers.getControllers()) {
            Gdx.app.log(this.getClass().getSimpleName(), controller.getName());
        }
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
        Gdx.app.log(this.getClass().getSimpleName(), buttonCode + "");
        return false;
    }
    
    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return false;
    }
    
    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        Gdx.app.log(this.getClass().getSimpleName(), axisCode + " " + value);
        return false;
    }
    
    @Override
    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
        Gdx.app.log(this.getClass().getSimpleName(), povCode + " " + value);
        return false;
    }
    
    @Override
    public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
        Gdx.app.log(this.getClass().getSimpleName(), sliderCode + " " + value);
        return false;
    }
    
    @Override
    public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
        Gdx.app.log(this.getClass().getSimpleName(), sliderCode + " " + value);
        return false;
    }
    
    @Override
    public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
        Gdx.app.log(this.getClass().getSimpleName(), accelerometerCode + " " + value);
        return false;
    }
    
}
