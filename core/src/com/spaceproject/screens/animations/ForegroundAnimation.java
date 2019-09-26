package com.spaceproject.screens.animations;

import com.badlogic.gdx.math.MathUtils;

public enum ForegroundAnimation {
    tree, delaunay, orbit, drop, crossNoise;
    
    public static ForegroundAnimation random() {
        return ForegroundAnimation.values()[MathUtils.random(ForegroundAnimation.values().length - 1)];
    }
}
