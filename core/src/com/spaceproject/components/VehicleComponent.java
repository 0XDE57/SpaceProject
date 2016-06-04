package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class VehicleComponent implements Component {
	// seed that generated ship
	public long seed = 0;

	// id to identify one vehicle from another
	public int id;
	
	// how fast to accelerate
	public float thrust;
	
	// maximum speed vehicle can achieve
	public float maxSpeed;
}
