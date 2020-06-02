package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.utility.SimpleTimer;

public class CannonComponent implements Component {
    
    public Vector2 anchorVec;
    
    public float aimAngle;
    
    public float damage;
    
    public int maxAmmo;
    
    public int curAmmo;
    
    public float velocity;
    
    public float acceleration;
    
    public int size;
    
    //timers
    public SimpleTimer timerFireRate;
    public SimpleTimer timerRechargeRate;
    
}
