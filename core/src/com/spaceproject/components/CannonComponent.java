package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class CannonComponent extends Component {
	
	public float damage;
	
	public int maxAmmo;
	
	public int curAmmo;
	
	public float velocity;
	
	public int size;
	
	public float fireRate;
	
	public float timeSinceLastShot;
	
	public float rechargeRate;
	
	public float timeSinceRecharge;
	
}
