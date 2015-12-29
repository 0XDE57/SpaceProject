package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class MovementComponent implements Component {
	//velocity is rate of change in x and y. DX,DY being how fast entity is traveling in that direction
	public final Vector2 velocity = new Vector2();
	
	//acceleration is rate of change in velocity
	public final Vector2 accel = new Vector2();
}
