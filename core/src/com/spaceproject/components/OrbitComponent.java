package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class OrbitComponent implements Component {
	//the entity to orbit
	public Entity parent; 
	
	//angle from parent in radians
	public double angle, startAngle;
	
	//orbit parameters
	public float radialDistance; //distance from entity to rotate
	public float tangentialSpeed; //how fast to orbit

	//how fast to rotate
	public float rotSpeed;

	//rotation direction
	public boolean rotateClockwise;
	
}
