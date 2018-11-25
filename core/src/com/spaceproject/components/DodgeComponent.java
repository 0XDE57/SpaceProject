package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Interpolation;
import com.spaceproject.utility.SimpleTimer;

public class DodgeComponent implements Component {

    public SimpleTimer animationTimer;

    public Interpolation animInterpolation;

    public float distance;
    public float traveled;

    public float direction;

    public enum FlipDir {
        left, right
    }
    public FlipDir dir;

    public int revolutions;

}
