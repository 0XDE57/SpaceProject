package com.spaceproject.config;


public class EntityConfig extends Config {
    //TODO: find better way to do entity config + generation

    //character
    public float characterHealth;
    public float characterWalkSpeed;
    
    //control
    public long controlTimerDodge;
    public long controlTimerVehicle;
    
    //ship
    public int shipSizeMin;
    public int shipSizeMax;
    public float shipHealth;
    public float engineThrust;
    public float dodgeForce;
    public long dodgeAnimationTimer;
    public int cannonSize;
    public int cannonAmmo;
    public int cannonDamage;
    public long cannonFireRate;
    public long cannonRechargeRate;
    public float cannonVelocity;
    public float cannonAcceleration;
    public long shrinkGrowAnimTime;
    
    
    @Override
    public void loadDefault() {
        characterHealth = 100;
        characterWalkSpeed = 200f;
        
        controlTimerVehicle = 1000;
        controlTimerDodge = 500;
        
        shipSizeMin = 10;
        shipSizeMax = 36;
        shipHealth = 200;
        engineThrust = 200;
        dodgeAnimationTimer = 475;
        dodgeForce = 5f;
        cannonSize = 1;
        cannonAmmo = 5;
        cannonDamage = 15;
        cannonFireRate = 200;
        cannonRechargeRate = 1000;
        cannonVelocity = 680;
        cannonAcceleration = 200;
        shrinkGrowAnimTime = 2500;
        
    }
    
}
