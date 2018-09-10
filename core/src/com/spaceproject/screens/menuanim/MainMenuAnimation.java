package com.spaceproject.screens.menuanim;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class MainMenuAnimation {
	public abstract void render(float delta, ShapeRenderer shape);
	public abstract void resize(int width, int height);
}
