package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class BarrelRollComponent implements Component {
    
    public enum FlipState {
        off, left, right, forward
    }
    
    public FlipState flipState;
    
    public SimpleTimer timeoutTimer;
    
    public SimpleTimer animationTimer;
    
    public int revolutions;
    
    public float force;
    
}
