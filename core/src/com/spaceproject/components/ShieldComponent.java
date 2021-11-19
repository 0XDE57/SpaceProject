package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.spaceproject.utility.SimpleTimer;

public class ShieldComponent implements Component {
    
    public enum State { off, on, charge, discharge }
    
    public State state = State.off;
    
    public boolean activate;
    
    public float defence;
    
    public float radius;
    
    public float maxRadius;
    
    public SimpleTimer animTimer;
    
    public Color color;
    
}
