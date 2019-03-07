package com.spaceproject.ui;

import com.badlogic.gdx.graphics.Texture;
import com.spaceproject.generation.TextureFactory;

public class SpaceBackgroundTile {
    public final float x;
    public final float y;
    public final float tileX;
    public final float tileY;
    public final float depth;
    public final int size;
    public final Texture tex;
    public final float scale;
    
    public TileType type;
    
    public enum TileType {
        Stars, Dust
    }
    
    public SpaceBackgroundTile(int tileX, int tileY, float renderDepth, int tileSize, TileType type) {
        
        this.tileX = tileX;
        this.tileY = tileY;
        this.type = type;
        depth = renderDepth;
        
        //generate texture
        switch (type) {
            case Stars:
                tex = TextureFactory.generateSpaceBackgroundStars(tileX, tileY, tileSize, renderDepth);
                scale = 1;
                break;
            case Dust:
                scale = 4;
                tileSize /= scale;
                tex = TextureFactory.generateSpaceBackgroundDust(tileX, tileY, tileSize);
                break;
            default:
                tex = null;
                scale = 1;
                break;
        }
        
        
        size = (int) (tileSize * scale);
        x = tileX * size;
        y = tileY * size;
        
    }
    
}
