package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Interpolation;
import com.spaceproject.utility.SimpleTimer;

public class BarrelRollComponent implements Component {
    
    public enum FlipDir {
        none, left, right
    }
    
    public FlipDir dir;
    
    public SimpleTimer rollTimer;
    
    public SimpleTimer animationTimer;
    
    public Interpolation animInterpolation;
    
    public int revolutions;
    
    public float force;
    
}
