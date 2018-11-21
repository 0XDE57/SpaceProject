package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class VehicleComponent implements Component {

	//TODO: should consider breaking into a DrivableComponent (to account for land vehicles) and engine/ship component for further properties.

	public Entity driver;

	// tempGenID to identify one vehicle from another
	@Deprecated
	public int id;//TODO: i dont think this should be used anymore -> seed? entity hashcode? IDComp?

	//TODO: implement engine state behavior
	enum EngineState {
		local, 	//intention: combat, interactions, land on planet, small distance travel (planet to planet within a star system)
				//behavior: small friction applied (unrealistic but practical?/ toggleable engine tuning?), velocity cap, slightly less thrust than travel mode

		travel,	//intention: combat evasion, exploration, large distance travel (star to star)
				//behavior: no friction,

		hyper	//intention: extra long distance star to star travel, autopilot/fast travel
				//behavior: very fast, no steering, no dodge, no shield, add blue shader/effect for blueshift
	}
	// how fast to accelerate
	public float thrust;
	
	// maximum speed vehicle can achieve
	public float maxSpeed;
	public static final int NOLIMIT = -1;//no max speed/infinite

}
