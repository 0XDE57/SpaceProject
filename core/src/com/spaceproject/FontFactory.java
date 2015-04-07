package com.spaceproject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class FontFactory {

	//fonts
	//public static String fontPressStart = "fonts/PressStart2P.ttf";
	//public static String fontdPuntillas = "fonts/dPuntillas.ttf";
	//public static String fontComfortaaLight = "fonts/ComfortaaLight.ttf";
	//public static String fontComfortaaRegular = "fonts/ComfortaaRegular.ttf";
	//public static String fontComfortaaBold = "fonts/ComfortaaBold.ttf";
	
	public static String fontBitstreamVM = "fonts/bitstream/VeraMono.ttf";
	public static String fontBitstreamVMBoldItalic = "fonts/bitstream/VeraMono-Bold-Italic.ttf";
	public static String fontBitstreamVMBold = "fonts/bitstream/VeraMono-Bold.ttf";
	public static String fontBitstreamVMItalic = "fonts/bitstream/VeraMono-italic.ttf";
	
	public static BitmapFont createFont(String font, int size) {
		FreeTypeFontGenerator generator;
		FreeTypeFontParameter parameter;
		BitmapFont fontPressStart;
		
		generator = new FreeTypeFontGenerator(Gdx.files.internal(font));
		parameter = new FreeTypeFontParameter();
		parameter.size = size;
		fontPressStart = generator.generateFont(parameter);
		generator.dispose();
		
		return fontPressStart;
	}
}
