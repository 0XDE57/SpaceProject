package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Rectangle;

public class BoundsComponent extends Component {
	//The bounding box for collision detection. Hitbox.
	public final Rectangle bounds = new Rectangle();

}
