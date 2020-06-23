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
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;

public class ControllerInputSystem extends EntitySystem implements ControllerListener {
    
    private ImmutableArray<Entity> players;
    
    public ControllerInputSystem() {
        Controllers.addListener(this);
        
        for (Controller controller : Controllers.getControllers()) {
            Gdx.app.log(this.getClass().getSimpleName(), controller.getName());
        }
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
        //Gdx.app.log(this.getClass().getSimpleName(), buttonCode + "");
        return false;
    }
    
    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return false;
    }
    
    float leftStickHorAxis;
    float leftStickVertAxis;
    float dist;
    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        //Gdx.app.log(this.getClass().getSimpleName(), axisCode + " " + value);
        float deadZone = 0.25f;
    
       
        
    
        
            if (axisCode == Xbox.L_STICK_HORIZONTAL_AXIS) {
                //if (value >= deadZone) {
                    leftStickHorAxis = value;
                //}
                //Gdx.app.log(this.getClass().getSimpleName(), "horizontal " + value);
            }
            if (axisCode == Xbox.L_STICK_VERTICAL_AXIS) {
                //if (value >= deadZone) {
                    leftStickVertAxis = value;
                //}
                //Gdx.app.log(this.getClass().getSimpleName(), "vertical " + value);
            }
            
            //Gdx.app.log(this.getClass().getSimpleName(), "d " + dist);
        
    
        dist = MyMath.distance(0, 0, leftStickHorAxis, leftStickVertAxis);
        ControllableComponent control = Mappers.controllable.get(players.first());
        if (dist >= deadZone) {
            
            control.angleTargetFace = MyMath.angleTo(0, 0, leftStickVertAxis, leftStickHorAxis) + 1.57f;
            control.movementMultiplier = MathUtils.clamp(dist, 0, 1);
            control.moveForward = true;
        } else {
            control.moveForward = false;
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
