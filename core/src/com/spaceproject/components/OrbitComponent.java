package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class OrbitComponent implements Component {
	//the entity to orbit
	public Entity parent; 
	
	//angle from parent position in radians 
	public double angle, startAngle;
	
	//how fast to orbit around entity
	public long msPerRevolution;
	public float tangentialVelocity;
	
	//how fast to rotate
	public float rotSpeed;
	
	//radialDistance from entity to rotate
	public float radialDistance;
	
	//rotation direction
	public boolean rotateClockwise;
	
}
