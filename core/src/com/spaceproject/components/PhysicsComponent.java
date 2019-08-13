package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.physics.box2d.Body;

public class PhysicsComponent implements Component {
    
    //The bounding box for collision detection. Hitbox.
    @Deprecated
    public Polygon poly;
    
    public Body body;
    
}
