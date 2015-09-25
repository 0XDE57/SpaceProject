package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class HealthComponent extends Component {
	//health for living things / combat, entity dies upon value reaching 0
	public float health;
	
	//total health
	public float maxHealth;
}
