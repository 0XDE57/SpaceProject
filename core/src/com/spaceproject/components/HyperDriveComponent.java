package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.utility.SimpleTimer;

public class HyperDriveComponent implements Component {
    
    public boolean active;
    
    public float speed;
    
    public final Vector2 velocity = new Vector2();
    
    public SimpleTimer coolDownTimer;
    
}
