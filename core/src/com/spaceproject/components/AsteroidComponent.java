package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Polygon;

public class AsteroidComponent implements Component {
    
    //public enum Composition { rock, ice }
    
    public enum Type {
        free, orbitLocked
    }
    
    public Type type;
    
    public Polygon polygon;
    
    public Color color; //temporary rendering color
    
    public float area;
    
}
