package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.config.LandConfig;

public class ScreenTransitionComponent implements Component {

	public static enum AnimStage {
		shrink,
		zoom,
		//screenfEffect
		transition,
		pause,
		exit;		
	}
	public AnimStage stage;

	public LandConfig landCFG;
	
	public float timer;
}
