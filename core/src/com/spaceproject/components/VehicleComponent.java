package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class VehicleComponent implements Component {
    
    public Entity driver;
    
    
    //TODO: implement engine state behavior
    enum EngineState {
        local,    //intention: combat, interactions, land on planet, small distance travel (planet to planet within a star system)
        //behavior: small friction applied (unrealistic but practical?/ toggleable engine tuning?), velocity cap, slightly less thrust than travel mode
        
        travel,    //intention: combat evasion, exploration, large distance travel (star to star)
        //behavior: no friction,
        
        hyper    //intention: extra long distance star to star travel, autopilot/fast travel
        //behavior: very fast, no steering, no dodge, no shield, add blue shader/effect for blueshift
    }
    
    // how fast to accelerate
    public float thrust;
    
    // maximum speed vehicle can
    public float hyperSpeed;
    public float maxSpeed = 60;
    public static final int NOLIMIT = -1;//no max speed/infinite
    
}
