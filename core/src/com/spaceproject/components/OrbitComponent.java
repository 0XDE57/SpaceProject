package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class OrbitComponent implements Component {
	//the entity to orbit
	public Entity parent; 
	
	//angle from parent position in radians 
	public float angle;
	
	//how fast to orbit around entity
	public float orbitSpeed;
	
	//how fast to rotate
	public float rotSpeed;
	
	//distance from entity to rotate
	public float distance;
	
	//rotation direction
	public boolean rotateClockwise;
	
}
