package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class ShieldComponent implements Component {

    public float defence;

    public float radius;

    boolean active;

    public SimpleTimer animTimer;

}
