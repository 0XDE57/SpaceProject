package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;


public class SplineComponent implements Component {
    
    //todo: change to vector3 to store depth index, (eg I want to render this line on top of all the others)
    public Vector2[] path;
    
    public int index;
    
    public Color color;
    
}
