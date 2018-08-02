package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class AIComponent implements Component {

	public Entity attackTarget;//short index...
	//public Array<Entity> attackTargets;
	
	public Entity followTarget;//short index...
	//public Array<Entity> followTargets;

	public Entity planetTarget; //test land

	public enum testState {
		dumbwander,
		attack,
		landOnPlanet,
		idle
	}
	public testState state;

	//statemachine?
	/*
	state {
		follow,
		attack,
		gotoPlace,
		idle,
		customTask -> delegate to logic
		...	
	}	
	 */
}
