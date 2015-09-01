package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;

public class BoundsComponent extends Component {
	
	//The bounding box for collision detection. Hitbox.	
	public Polygon poly;

}
