package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.utility.SimpleTimer;

public class DodgeComponent implements Component {
    
    public SimpleTimer animationTimer;
    
    public Interpolation animInterpolation;
    
    
    public float direction;
    public float force;
    
    public enum FlipDir {
        left, right
    }
    
    public FlipDir dir;
    
    public int revolutions;
    
}
