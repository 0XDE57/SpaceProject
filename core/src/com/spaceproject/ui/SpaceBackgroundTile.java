package com.spaceproject.ui;

import com.badlogic.gdx.graphics.Texture;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.EngineConfig;
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
                scale = 1.0f / SpaceProject.configManager.getConfig(EngineConfig.class).renderScale;
                break;
            case Dust:
                int altScale = 4;
                this.scale = altScale / SpaceProject.configManager.getConfig(EngineConfig.class).renderScale;
                tileSize /= altScale;
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
