package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;

public class MapComponent implements Component {
	/* color of marker */
	public Color color;
	
	/* radialDistance entity must be from player to show up on drawMap */
	public int distance;
}
