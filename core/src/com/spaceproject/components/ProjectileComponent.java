package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class ProjectileComponent extends Component {
	
	public int maxAmmo;
	
	public int curAmmo;
	
	public float velocity;
	
	public int size;
	
	public float fireRate;
	
	public float timeSinceLastShot;
	
	public float rechargeRate;
	
	public float timeSinceRechage;
}
