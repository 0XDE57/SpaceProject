package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.spaceproject.utility.SimpleTimer;

public class ShieldComponent implements Component {

    public float defence;

    public float radius;

    public float maxRadius;

    public boolean active;

    public boolean growing;

    public SimpleTimer animTimer;

    public Color color;


}
