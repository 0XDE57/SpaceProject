package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class TransformComponent implements Component {
	/* Position in world x, y. Z used for rendering order */
	public final Vector2 pos = new Vector2();

	/* velocity is rate of change in x and y */
	public final Vector2 velocity = new Vector2();

	/* acceleration is rate of change in velocity */
	public final Vector2 accel = new Vector2();

	/* Orientation in radians */
	public float rotation = 0.0f;

	public byte zOrder;
}
