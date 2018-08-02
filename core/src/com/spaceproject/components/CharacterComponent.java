package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class CharacterComponent implements Component {

	public float walkSpeed;

	public Entity vehicle;
	//public SimpleTimer timerVehicle;// = new SimpleTimer(1500);
}
