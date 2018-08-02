package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

/**
 * The barycenter is the center of mass of two or more bodies that orbit each other and is the point about which the bodies orbit.
 * in our use case, this will be the "celestial anchor" in where an entity holding this component will be
 * act as the anchor
 */
public class BarycenterComponent implements Component {
    public long seed;

    public AstronomicalBodyType bodyType;
    public enum AstronomicalBodyType {
        singleStarPlanetary,
        multiStellar,
        multiStellarPlanetary,
        rouguePlanet,
        lonestar
    }
}
