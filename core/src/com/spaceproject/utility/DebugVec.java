package com.spaceproject.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

public class DebugVec {
    public Vector2 pos;
    public Vector2 vec;
    public Color color;
    //todo: color a color b
    //todo: project
    
    public DebugVec(Vector2 pos, Vector2 vec, Color color) {
        this.pos = pos;
        this.vec = vec;
        this.color = color;
    }
}
