package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class TractorBeamComponent implements Component {

    public enum State {
        off, push, pull;
    }

    public State state = State.off;

    public float maxDist;

    public float magnitude;

}
