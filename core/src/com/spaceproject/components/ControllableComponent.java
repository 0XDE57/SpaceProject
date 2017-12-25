package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class ControllableComponent implements Component {
	
	//movement
	public boolean moveForward; 
	public boolean moveLeft;
	public boolean moveRight;
	public boolean moveBack;
	
	public float movementMultiplier;//analog control [0-1]
	public float angleFacing;
	
	//actions
	public boolean shoot;
	public boolean changeVehicle;
	public boolean transition;
	public boolean canTransition;
	
	//timers
	public float timeSinceVehicle;
	public int timeTillCanGetInVehicle = 60;
	
}