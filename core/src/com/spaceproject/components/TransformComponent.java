package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class TransformComponent extends Component {
	/* Position in world x, y. Z used for rendering order */
	public final Vector3 pos = new Vector3();
	/* Orientation in radians */
	public float rotation = 0.0f;
}
