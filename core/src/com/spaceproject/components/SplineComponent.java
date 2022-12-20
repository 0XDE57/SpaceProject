package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;


public class SplineComponent implements Component {
    
    //xy = position, z = velocity
    public Vector3[] path;
    
    // states: damage, off, on, boost, hyper -> -1, 0, 1, 2, 3
    public byte[] state;
    
    //the current head of the path
    public int indexHead;
    
    public int zOrder;
    
    public Style style;
    
    public enum Style {
        solid, velocity, state, rainbow
    }
    
    public Color color;
    
}
