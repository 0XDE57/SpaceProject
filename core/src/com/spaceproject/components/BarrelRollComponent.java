package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Interpolation;
import com.spaceproject.utility.SimpleTimer;

public class BarrelRollComponent implements Component {
    
    public SimpleTimer timerDodge;
    
    public SimpleTimer animationTimer;
    
    public Interpolation animInterpolation;
    
    
    //public float direction;
    public float force;
    
    public enum FlipDir {
        none, left, right
    }
    
    public FlipDir dir;
    
    public int revolutions;
    
}
