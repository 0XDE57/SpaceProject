package com.spaceproject.generation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class FontFactory {

	//fonts
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
		
		generator = new FreeTypeFontGenerator(Gdx.files.internal(font));
		parameter = new FreeTypeFontParameter();
		parameter.size = size;
		newFont = generator.generateFont(parameter);
		generator.dispose();
		
		return newFont;
	}
}
