package com.spaceproject.components;


import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class AISpawnComponent implements Component {
    
    public SimpleTimer[] timers;
    public long min;
    public long max;
    public AIComponent.State state;
    public int spawnCount;
    
}
