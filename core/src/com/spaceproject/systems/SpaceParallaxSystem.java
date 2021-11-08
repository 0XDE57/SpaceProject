package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.SpaceBackgroundTile;
import com.spaceproject.ui.SpaceBackgroundTile.TileType;
import com.spaceproject.utility.SimpleTimer;

import java.util.ArrayList;

public class SpaceParallaxSystem extends EntitySystem implements Disposable {
    private final OrthographicCamera cam;
    private final SpriteBatch spriteBatch;
    
    // background layer of tiles
    private final ArrayList<SpaceBackgroundTile> tiles = new ArrayList<>();;
    
    // multiplier for parallax position of tile
    private static float dustTileDepth = 0.01f;
    private static float bgTileDepth = 0.02f; // background
    private static float fgTileDepth = 0.018f; // foreground
    
    // center tile to check for tile change
    private Vector2 dustCenterTile;
    private Vector2 bgCenterTile; // background
    private Vector2 fgCenterTile; // foreground
    
    private static final int tileSize = 512;//1024;
    private int surround = 2;// how many tiles to load around center tile
    
    // timer for how often to check if player moved tiles
    private SimpleTimer checkTileTimer;
    
    Matrix4 projectionMatrix = new Matrix4();
    
    public SpaceParallaxSystem() {
        cam = new OrthographicCamera();
        spriteBatch = new SpriteBatch();
     
        checkTileTimer = new SimpleTimer(500);
    }
    
    @Override
    public void update(float delta) {
        // load and unload tiles
        updateTiles(delta);
        
        float parallaxMultiplier = 1f;//-0.5f;
        cam.position.x = GameScreen.cam.position.x * parallaxMultiplier;
        cam.position.y = GameScreen.cam.position.y * parallaxMultiplier;
        //cam.zoom = GameScreen.cam.zoom;
        //cam.update();
        
        //spriteBatch.setProjectionMatrix(projectionMatrix);
        //spriteBatch.setProjectionMatrix(GameScreen.cam.combined);
        spriteBatch.begin();
        drawParallaxTiles();
        spriteBatch.end();
    }
    
    
    private void drawParallaxTiles() {
        for (SpaceBackgroundTile tile : tiles) {
            //draw = (tile position + (cam position - center of tile)) * depth
            //float drawX = tile.x - (cam.position.x - (tile.size * 0.5f)) * tile.depth;
            //float drawY = tile.y - (cam.position.y - (tile.size * 0.5f)) * tile.depth;
            //float drawX = (tile.x * tile.depth) - cam.position.x;
            //float drawY = (tile.y * tile.depth) - cam.position.y;
            float drawX = tile.x - cam.position.x * tile.depth;
            float drawY = tile.y - cam.position.y * tile.depth;
            drawX += (Gdx.graphics.getWidth()/2);
            drawY += (Gdx.graphics.getHeight()/2);
            
            //draw texture
            float width = tile.tex.getWidth();
            float height = tile.tex.getHeight();
            spriteBatch.draw(tile.tex, drawX, drawY,
                    0, 0,
                    width, height,
                    tile.scale, tile.scale,
                    0, 0, 0, (int) width, (int) height, false, false);
        }
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
    
        //Vector2 currentTile = getTilePos(-cam.position.x, -cam.position.y, dustTileDepth, SpaceBackgroundTile.TileType.Dust);
        //DebugSystem.addDebugText(-cam.position.x + ", " + -cam.position.y +  " -> " + currentTile.x + ", " + currentTile.y, 50, 50, false);
        
        // timer to check when player has changed tiles
        //if (checkTileTimer.canDoEvent()) {
        //    checkTileTimer.reset();
            
            //update each layer in order of depth
            dustCenterTile = updateLayer(dustCenterTile, dustTileDepth, SpaceBackgroundTile.TileType.Dust);
            bgCenterTile = updateLayer(bgCenterTile, bgTileDepth, SpaceBackgroundTile.TileType.Stars);
            fgCenterTile = updateLayer(fgCenterTile, fgTileDepth, SpaceBackgroundTile.TileType.Stars);
        //}
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
        //Vector2 currentTile = getTilePos(cam.position.x, cam.position.y, depth, type);
        Vector2 currentTile = getTilePos(cam.position.x, cam.position.y, depth, type);
        
        //Gdx.app.debug(this.getClass().getSimpleName(),cam.position.x + ", " + cam.position.y + " -> " + currentTile.x + ", " + currentTile.y);
        
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
     * @param type
     * @return tile that an object is in.
     */
    private static Vector2 getTilePos(float posX, float posY, float depth, TileType type) {
        //TODO: clean this up. also dust tiles have an overlap rendering issue
        // calculate position
        /*
        float scale = 1.0f;
        switch (type) {
            case Dust:
                scale = 4.0f;
                break;
            case Stars:
                scale = 1.0f;
                break;
        }
        int size = (int)((tileSize / scale) * (scale / SpaceProject.configManager.getConfig(EngineConfig.class).renderScale));*/
        //float x = posX - (posX - (tileSize * 0.5f)) * depth;
        //float y = posY - (posY - (tileSize * 0.5f)) * depth;
        //float x = posX - (posX - (tileSize)) * depth;
        //float y = posY - (posY - (tileSize)) * depth;
        float x = (posX + tileSize) * depth;
        float y = (posY + tileSize) * depth;
        //float x = posX - (tileSize * 0.5f) * depth;
        //float y = posY - (tileSize * 0.5f) * depth;
        
        // calculate tile that position is in
        int tX = (int)(x / tileSize);
        int tY = (int)(y / tileSize);
        
        // subtract 1 from tile position if less than zero
        // to account for -1/x giving 0
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
                //for (int index = 0; index < tiles.size(); ++index) {
                for (int index = 0; index < tiles.size() && !exists; ++index) {
                    SpaceBackgroundTile t = tiles.get(index);
                    //TODO: explore an ID method of checking existence
                    if (t.tileX == tX && t.tileY == tY && t.depth == depth && t.type == type) {
                        exists = true;
                        //break;
                    }
                }
                
                // create and add tile if doesn't exist
                if (!exists) {
                    Gdx.app.debug(this.getClass().getSimpleName(), "Load " + type + " tile: [" + depth + "]: " + tX + ", " + tY);
                    tiles.add(new SpaceBackgroundTile(tX, tY, depth, tileSize, type));
                }
            }
        }
        
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
                    Gdx.app.debug(this.getClass().getSimpleName(), "Unload " + tile.type + " tile: [" + depth + "]: " + tile.tileX + ", " + tile.tileY);
                    
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
    
    
    @Override
    public void dispose() {
        //dispose of textures
        for (SpaceBackgroundTile t : tiles) {
            t.tex.dispose();
        }
        tiles.clear();
    }
    
}
