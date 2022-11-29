package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;


public class SplineComponent implements Component {
    
    public Vector3[] path;
    
    public int index;
    
    public Color color;
    
    public int zOrder;
    
    public Style style;
    
    public enum Style {
        solid, velocity, rainbow
    }
    
}
