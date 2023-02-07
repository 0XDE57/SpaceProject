package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class StarComponent implements Component {
    
    public float radius;
    
    //public double mass;
    
    //public double luminosity;
    
    //temperature of star typically (2,000K - 40,000K)
    public double temperature; //K
    
    public double peakWavelength; //nanometers
    
    public float[] colorTemp; // Black Body Radiation!
    
    //public long age; //years
    
}
