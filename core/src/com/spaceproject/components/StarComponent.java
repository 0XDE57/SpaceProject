package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class StarComponent implements Component {
    
    public double mass;
    
    public double radius;
    
    public double luminosity;
    
    //temperature of star typically (2,000K - 40,000K)
    public double temperature; //K
    
    public long age; //years
    
}
