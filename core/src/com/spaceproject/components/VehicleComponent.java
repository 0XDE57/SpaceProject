package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class VehicleComponent implements Component {



	public Entity driver;
	
	// seed that generated ship
	//public long seed = 0;

	// tempGenID to identify one vehicle from another
	public int id;
	
	// how fast to accelerate
	public float thrust;
	
	// maximum speed vehicle can achieve
	public float maxSpeed;
	public static final int NOLIMIT = -1;//no max speed/infinite
}
