package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.spaceproject.utility.SimpleTimer;

import java.util.HashMap;

public class VehicleComponent implements Component {
    
    public Entity driver;
    
    public float thrust; //move to engine component as sub entity for ship?
    
    public Rectangle dimensions;
    
    public SimpleTimer weaponSwapTimer;

    public enum Tool {
        cannon, laser;
        //, growcannon
    }

    public Tool currentTool = Tool.cannon;

    public HashMap<Integer, Component> tools = new HashMap();
    
}
