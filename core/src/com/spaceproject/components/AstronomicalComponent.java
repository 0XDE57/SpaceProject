package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class AstronomicalComponent implements Component {

    public Classification classification;
    public enum Classification {
        star,
        planet,
        moon,
        satellite,
        asteroid
    }
}
