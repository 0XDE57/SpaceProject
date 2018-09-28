package com.spaceproject;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.screens.MyScreenAdapter;

public class SpaceProject extends Game {

	public static final String TITLE = "a space project";
	public static long SEED = 4; //test seed

	private static boolean isMobile;

	public static CelestialConfig celestcfg;
	public static KeyConfig keycfg;

	//TODO: put into a config
	public static float scale = 4.0f;
	public static int tileSize = 32;
	public static int chunkSize = 8;

	@Override
	public void create() {	
		MyScreenAdapter.game = this;

		isMobile = Gdx.app.getType() != Application.ApplicationType.Desktop;

		//load values for things like key mapping, settings, default values for generation
		loadConfigs();
				
	
		boolean inSpace = true;
		//setScreen(new GameScreen(inSpace));
		//setScreen(new Test3DScreen());
		//setScreen(new TestShipGenerationScreen(this));
		//setScreen(new TestNoiseScreen(this));
		//setScreen(new TestVoronoiScreen());
		setScreen(new TitleScreen(this));
	}
	
	private static void loadConfigs() {
		//keycfg = (KeyConfig) new KeyConfig().loadFromJson();
		keycfg = new KeyConfig();
		keycfg.loadDefault();
		

		//celestcfg = (CelestialConfig) new CelestialConfig().loadFromJson();
		celestcfg = new CelestialConfig();
		celestcfg.loadDefault();
		
		
		//WorldConfig worldcfg = (WorldConfig) new WorldConfig().loadFromJson();
		//worldcfg.loadDefault();
		//worldcfg.saveToJson();
	}

	public static boolean isMobile() {
		return isMobile;
	}
}
