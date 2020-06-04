package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class ControllableComponent implements Component {
    
    //movement
    public boolean moveForward;
    public boolean moveLeft;
    public boolean moveRight;
    public boolean moveBack;
    
    public float movementMultiplier;//analog control [0-1]
    public float angleTargetFace;
    
    //actions
    public boolean attack;
    public boolean defend;
    public boolean changeVehicle;
    public boolean transition;
    public boolean canTransition;
    public boolean alter;
    
    public boolean actionA;
    
    //timers
    public SimpleTimer timerVehicle;
    public SimpleTimer timerDodge;
    public SimpleTimer actionACooldownTimer;
}