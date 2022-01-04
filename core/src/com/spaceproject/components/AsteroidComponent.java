package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class AsteroidComponent implements Component {
    
    //public enum Composition { rock, ice } todo: different asteroid types?
    
    public Entity parentOrbitBody;
    
    public Polygon polygon;
    
    public Vector2 centerOfMass;
    
    public float area;
    
    public boolean doShatter = false;
    
    public Color debugColor; //temporary rendering color
    
}
