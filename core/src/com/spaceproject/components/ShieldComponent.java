package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.spaceproject.utility.SimpleTimer;

public class ShieldComponent implements Component {
    
    public float defence;
    
    public float radius;
    
    public float maxRadius;
    
    public boolean isActive;
    
    public boolean isCharging;
    
    public boolean isDischarging;
    //or
    public State state = State.off;
    public enum State { off, on, charge, discharge }
    
    public SimpleTimer animTimer;
    
    public Color color;
    
    
}
