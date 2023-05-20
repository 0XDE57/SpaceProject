package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class RespawnComponent implements Component {
    
    public String reason;

    public boolean spawn;

    public SimpleTimer timeout;

}
