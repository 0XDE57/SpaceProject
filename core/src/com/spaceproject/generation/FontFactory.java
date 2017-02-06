package com.spaceproject.generation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class FontFactory {

	// fonts
	public static final String fontPressStart = "fonts/pressstart/PressStart2P.ttf";

	public static final String fontComfortaaLight = "fonts/comfortaa/ComfortaaLight.ttf";
	public static final String fontComfortaaRegular = "fonts/comfortaa/ComfortaaRegular.ttf";
	public static final String fontComfortaaBold = "fonts/comfortaa/ComfortaaBold.ttf";

	public static final String fontBitstreamVM = "fonts/bitstream/VeraMono.ttf";
	public static final String fontBitstreamVMBoldItalic = "fonts/bitstream/VeraMono-Bold-Italic.ttf";
	public static final String fontBitstreamVMBold = "fonts/bitstream/VeraMono-Bold.ttf";
	public static final String fontBitstreamVMItalic = "fonts/bitstream/VeraMono-italic.ttf";

	public static BitmapFont createFont(String font, int size) {
		FreeTypeFontGenerator generator;
		FreeTypeFontParameter parameter;
		BitmapFont newFont;

		try {
			generator = new FreeTypeFontGenerator(Gdx.files.internal(font));
			parameter = new FreeTypeFontParameter();
			parameter.size = size;
			newFont = generator.generateFont(parameter);
			generator.dispose();
			return newFont;
		} catch (GdxRuntimeException ex) {
			System.err.println(ex.getMessage());
			System.out.println("Font not found: " + Gdx.files.getLocalStoragePath() + font);
			System.out.println("Make sure 'android/assets' is appended to run configuration. ");
		}

		System.out.println("Loaded default font.");
		return new BitmapFont();

	}
}
