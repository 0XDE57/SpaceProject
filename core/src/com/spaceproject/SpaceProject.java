package com.spaceproject;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.config.UIConfig;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.screens.MyScreenAdapter;

public class SpaceProject extends Game {

	public static final String TITLE = "a space project";
	public static final String VERSION = "pre-alpha";//<α

	public static long SEED = 4; //test seed

	private static boolean isMobile;

	public static EntityConfig entitycfg;
	public static CelestialConfig celestcfg;
	public static WorldConfig worldcfg;
	public static UIConfig uicfg;
	public static KeyConfig keycfg;


	@Override
	public void create() {	
		MyScreenAdapter.game = this;

		isMobile = Gdx.app.getType() != Application.ApplicationType.Desktop;

		//load values for things like key mapping, settings, default values for generation
		loadConfigs();


		setScreen(new TitleScreen(this));
	}
	
	private static void loadConfigs() {
		entitycfg = new EntityConfig();
		entitycfg.loadDefault();


		//keycfg = (KeyConfig) new KeyConfig().loadFromJson();
		keycfg = new KeyConfig();
		keycfg.loadDefault();
		

		//celestcfg = (CelestialConfig) new CelestialConfig().loadFromJson();
		celestcfg = new CelestialConfig();
		celestcfg.loadDefault();


		worldcfg = new WorldConfig();
		worldcfg.loadDefault();


		uicfg = new UIConfig();
		uicfg.loadDefault();
	}

	public static boolean isMobile() {
		return isMobile;
	}
}
