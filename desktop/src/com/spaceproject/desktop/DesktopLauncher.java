package com.spaceproject.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.spaceproject.SpaceProject;

public class DesktopLauncher {
	public static void main(String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		// set window to borderless
		// System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
		// config.resizable = false;

		// MSAA ---experiment with this later
		// http://www.badlogicgames.com/wordpress/?p=2071
		config.samples = 8;

		// set foreground and background fps to disable throttling to 60 frames
		config.vSyncEnabled = true;
		config.foregroundFPS = 0;
		config.backgroundFPS = 0;
		config.useGL30 = false;

		// initial/default window size
		config.width = 1280;
		config.height = 720;

		new LwjglApplication(new SpaceProject(), config);
	}
}
