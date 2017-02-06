package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.config.LandConfig;

public class ScreenTransitionComponent implements Component {
	public static enum AnimStage {
		stopShip, shrink, zoom, transition;
		//screenfEffect
	}
	public AnimStage stage;
	
	public LandConfig landCFG;
}
