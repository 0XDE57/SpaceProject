package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.spaceproject.utility.SimpleTimer;


public class GrowCannonComponent implements Component {

    public Entity missle;

    public float baseDamage;

    public float velocity;

    public float size;

    public float maxSize;

    public SimpleTimer growRateTimer;

    public boolean isCharging;
}
