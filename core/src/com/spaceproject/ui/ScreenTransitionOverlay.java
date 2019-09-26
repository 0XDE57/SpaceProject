package com.spaceproject.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.spaceproject.utility.SimpleTimer;

public class ScreenTransitionOverlay {
    
    private SimpleTimer fadeTimer;
    private FadeState fadeState;
    private int fadeTime = 2000;
    private Color fadeColor = new Color(1, 1, 1, 1);
    private ShapeRenderer shape = new ShapeRenderer();
    
    public ScreenTransitionOverlay() {
        fadeState = FadeState.off;
        fadeTimer = new SimpleTimer(fadeTime, false);
    }
    
    public void fadeIn() {
        fadeState = FadeState.fadeIn;
        fadeTimer.reset();
    }
    
    public void fadeOut() {
        fadeState = FadeState.fadeOut;
        fadeTimer.reset();
    }
    
    public FadeState getFadeState() {
        return fadeState;
    }
    
    public void render() {
        if (fadeState == FadeState.off) return;
    
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(fadeColor);
        switch (fadeState) {
            case on:
                fadeColor.a = 1;
                break;
            case fadeIn:
                fadeColor.a = fadeTimer.ratio();
                break;
            case fadeOut:
                fadeColor.a = 1 - fadeTimer.ratio();
                break;
        }
    
        shape.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        if (fadeTimer.tryEvent()) {
            switch (fadeState) {
                case fadeIn: fadeState = FadeState.on; break;
                case fadeOut: fadeState = FadeState.off; break;
            }
        }
    }
    
}
