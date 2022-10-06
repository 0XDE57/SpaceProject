package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class AsteroidBeltComponent implements Component {
    
    //distance from parent body
    public float radius;
    
    //width of band centered on radius
    public float bandWidth;
    
    public boolean clockwise;
    
    public float velocity;
    
    public int spawned;
    
    public int maxSpawn;
    
}
