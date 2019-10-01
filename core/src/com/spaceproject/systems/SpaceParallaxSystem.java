package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.SpaceBackgroundTile;
import com.spaceproject.ui.SpaceBackgroundTile.TileType;
import com.spaceproject.utility.SimpleTimer;

import java.util.ArrayList;

public class SpaceParallaxSystem extends EntitySystem implements Disposable {
    //TODO: loading, unloading tiles is slow, blame is probably pixmap generation, should try to replace with shader
    
    //private Engine engine;
    private static OrthographicCamera cam;
    
    // background layer of tiles
    private static ArrayList<SpaceBackgroundTile> tiles;
    
    // multiplier for parallax position of tile
    private static float dustTileDepth = 0.99f;
    private static float bgTileDepth = 0.9f; // background
    private static float fgTileDepth = 0.8f; // foreground
    
    // center tile to check for tile change
    private Vector2 dustCenterTile;
    private Vector2 bgCenterTile; // background
    private Vector2 fgCenterTile; // foreground
    
    private static int tileSize = 1024; // how large a tile texture is
    private int surround = 1;// how many tiles to load around center tile
    
    // timer for how often to check if player moved tiles
    private SimpleTimer checkTileTimer;
    
    public SpaceParallaxSystem() {
        this(MyScreenAdapter.cam);
    }
    
    public SpaceParallaxSystem(OrthographicCamera camera) {
        cam = camera;
        
        tiles = new ArrayList<SpaceBackgroundTile>();
        
        checkTileTimer = new SimpleTimer(500);
    }
    
    @Override
    public void update(float delta) {
        
        // load and unload tiles
        updateTiles(delta);
        
        // load tiles/spacedust/background clouds(noise/fractals)
    }
    
    
    /**
     * If camera has changed position relative to tiles, unload far tiles and
     * load near tiles.
     *
     * @param delta
     */
    private void updateTiles(float delta) {
        // TODO: consider adding timers to break up the process from happening
        // in one frame causing a freeze/jump
        // because putting it in a separate thread is not working (possible?)
        // due to glContext...
        //https://github.com/libgdx/libgdx/wiki/Threading
        //https://www.opengl.org/wiki/OpenGL_and_multithreading
        
        // timer to check when player has changed tiles
        if (checkTileTimer.canDoEvent()) {
            checkTileTimer.reset();
            
            //update each layer in order of depth
            dustCenterTile = updateLayer(dustCenterTile, dustTileDepth, SpaceBackgroundTile.TileType.Dust);
            bgCenterTile = updateLayer(bgCenterTile, bgTileDepth, SpaceBackgroundTile.TileType.Stars);
            fgCenterTile = updateLayer(fgCenterTile, fgTileDepth, SpaceBackgroundTile.TileType.Stars);
        }
    }
    
    /**
     * Checks if correct tiles are loaded around camera position.
     * Old tiles too far away from the camera are unloaded.
     * New tiles are loaded to surround camera.
     *
     * @param lastTile previous tile that was loaded
     * @param depth    layer of tile
     * @param type     of tile
     * @return position of current tile
     */
    private Vector2 updateLayer(Vector2 lastTile, float depth, SpaceBackgroundTile.TileType type) {
        //calculate tile camera is within
        Vector2 currentTile = getTilePos(cam.position.x, cam.position.y, depth);
        
        //load initial tiles
        if (lastTile == null) {
            lastTile = currentTile;
            loadTiles(currentTile, depth, type);
        }
        
        //check if moved tile
        if (currentTile.x != lastTile.x || currentTile.y != lastTile.y) {
            
            // unload old tiles
            unloadTiles(currentTile, depth);
            
            // load new tiles
            loadTiles(currentTile, depth, type);
            
        }
        
        return currentTile;
    }
    
    
    /**
     * Convert world position to tile position.
     *
     * @param posX
     * @param posY
     * @return tile that an object is in.
     */
    public static Vector2 getTilePos(float posX, float posY, float depth) {
        // calculate position
        int x = (int) (posX - (cam.position.x - (tileSize / 2)) * depth);
        int y = (int) (posY - (cam.position.y - (tileSize / 2)) * depth);
        
        // calculate tile that position is in
        int tX = x / tileSize;
        int tY = y / tileSize;
        
        // subtract 1 from tile position if less than zero to account for -1/x
        // giving 0
        if (x < 0) {
            --tX;
        }
        if (y < 0) {
            --tY;
        }
        
        return new Vector2(tX, tY);
    }
    
    /**
     * Load tiles surrounding centerTile of specified depth.
     *
     * @param centerTile
     * @param depth
     * @param type
     */
    private void loadTiles(Vector2 centerTile, float depth, TileType type) {
        for (int tX = (int) centerTile.x - surround; tX <= centerTile.x + surround; tX++) {
            for (int tY = (int) centerTile.y - surround; tY <= centerTile.y + surround; tY++) {
                // check if tile already exists
                boolean exists = false;
                for (int index = 0; index < tiles.size() && !exists; ++index) {
                    SpaceBackgroundTile t = tiles.get(index);
                    //TODO: explore an ID method of checking existence
                    if (t.tileX == tX && t.tileY == tY && t.depth == depth && t.type == type) {
                        exists = true;
                    }
                }
                
                // create and add tile if doesn't exist
                if (!exists) {
                    tiles.add(new SpaceBackgroundTile(tX, tY, depth, tileSize, type));
                }
            }
        }
        //System.out.println("Load " + type + " tile: [" + depth + "]: " + (int)centerTile.x + ", " + (int)centerTile.y);
    }
    
    /**
     * Remove any tiles not surrounding centerTile of same depth.
     *
     * @param centerTile
     * @param depth
     */
    private void unloadTiles(Vector2 centerTile, float depth) {
        for (int index = 0; index < tiles.size(); ++index) {
            SpaceBackgroundTile tile = tiles.get(index);
            if (tile.depth == depth) {
                if (tileIsNear(centerTile, tile)) {
                    
                    // dispose the texture so it doesn't eat up memory
                    tile.tex.dispose();
                    // remove tile
                    tiles.remove(index);
                    
                    // reset search index because removing elements changes
                    // position of elements
                    index = -1;
                    if (index >= tiles.size()) {
                        continue;
                    }
                }
            }
        }
    }
    
    /**
     * Check if a tile is within range of center tile.
     */
    private boolean tileIsNear(Vector2 centerTile, SpaceBackgroundTile tileToCheck) {
        return tileToCheck.tileX < centerTile.x - surround || tileToCheck.tileX > centerTile.x + surround
                || tileToCheck.tileY < centerTile.y - surround || tileToCheck.tileY > centerTile.y + surround;
    }
    
    public static ArrayList<SpaceBackgroundTile> getTiles() {
        return tiles;
    }
    
    @Override
    public void dispose() {
        //dispose of textures
        for (SpaceBackgroundTile t : tiles) {
            t.tex.dispose();
        }
        tiles.clear();
        
    }
}
