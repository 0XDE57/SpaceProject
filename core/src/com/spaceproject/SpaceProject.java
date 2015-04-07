package com.spaceproject;

import com.badlogic.gdx.Game;
import com.spaceproject.screens.MainMenuScreen;
import com.spaceproject.screens.SpaceScreen;
import com.spaceproject.screens.TestShipGenerationScreen;

public class SpaceProject extends Game {

	@Override
	public void create() {
		
		//setScreen(new TestShipGenerationScreen(this));
		
		setScreen(new SpaceScreen(this));
		
		//setScreen(new MainMenuScreen(this));
	}
	
}
