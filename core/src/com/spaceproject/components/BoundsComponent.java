package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Polygon;

public class BoundsComponent implements Component {
	
	//The bounding box for collision detection. Hitbox.	
	public Polygon poly;

}
