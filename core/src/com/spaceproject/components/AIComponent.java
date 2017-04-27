package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class AIComponent implements Component {

	public Entity attackTarget;//short index...
	//public ArrayList<Entity> attackTargets;
	
	public Entity followTarget;//short index...
	//public ArrayList<Entity> followTargets;
	
	//statemachine?
	/*
	state {
		follow,
		attack,
		gotoPlace,
		idle,
		...	
	}	
	 */
}
