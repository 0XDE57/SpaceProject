package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class CircumstellarDiscComponent implements Component {
    
    //distance from parent body
    public float radius;
    
    //width of band centered on radius
    public float width;
    
    public boolean clockwise;
    
    public float velocity;
    
}
