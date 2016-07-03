package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class TransformComponent implements Component {
	/* Position in world x, y. Z used for rendering order */
	public final Vector3 pos = new Vector3();
	
	/* Orientation in radians */
	public float rotation = 0.0f;
	
	/* velocity is rate of change in x and y */
	public final Vector2 velocity = new Vector2();

	/* acceleration is rate of change in velocity */
	public final Vector2 accel = new Vector2();
}
