package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.config.LandConfig;

public class ScreenTransitionComponent implements Component {

	public enum TakeOffAnimStage {
		//fly/hover animation,
		//screenfEffect.
		transition,
		sync,
		zoomOut,
		grow,
		end
	}
	
	public enum LandAnimStage {
		shrink,
		zoomIn,
		//screenfEffect
		transition,
		pause,
		exit,
		end
	}
	
	public LandAnimStage landStage;
	public LandAnimStage curLandStage;
	public TakeOffAnimStage takeOffStage;
	public TakeOffAnimStage curTakeOffStage;

	public LandConfig landCFG;
	
	public float timer;
}
