package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.config.LandConfig;

public class ScreenTransitionComponent implements Component {

	public static enum TakeOffAnimStage {
		//fly
		transition,
		zoomOut,
		grow,
		end;
	}
	
	public static enum LandAnimStage {
		shrink,
		zoomIn,
		//screenfEffect
		transition,
		pause,
		exit,
		end;		
	}
	
	public LandAnimStage landStage;
	public TakeOffAnimStage takeOffStage;

	public LandConfig landCFG;
	
	public float timer;
}
