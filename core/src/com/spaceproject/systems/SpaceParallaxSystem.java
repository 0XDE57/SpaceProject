package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.SpaceBackgroundTile;
import com.spaceproject.ui.SpaceBackgroundTile.TileType;

import java.util.ArrayList;

public class SpaceParallaxSystem extends EntitySystem implements Disposable {
    
    private final OrthographicCamera cam;
    private final SpriteBatch spriteBatch;
    
    // background layer of tiles
    private final ArrayList<SpaceBackgroundTile> tiles = new ArrayList<>();;
    
    // multiplier for parallax position of tile
    private static float dustTileDepth = 0.01f;
    private static float star1TileDepth = 0.02f;
    private static float star2TileDepth = 0.018f;
    private static float star3TileDepth = 0.2f;
    
    // center tile to check for tile change
    private Vector2 dustCenterTile;
    private Vector2 star0CenterTile;
    private Vector2 star1CenterTile;
    private Vector2 star2CenterTile;
    private Vector2 star3CenterTile;
    
    private static final int tileSize = 512;//1024;
    private int surround = 2;// how many tiles to load around center tile
    private final ShaderProgram spaceShader;
    
    
    public SpaceParallaxSystem() {
        cam = new OrthographicCamera();
        spriteBatch = new SpriteBatch();
        
        spaceShader = new ShaderProgram(Gdx.files.internal("shaders/spaceParallax.vert"), Gdx.files.internal("shaders/spaceParallax.frag"));
        if (spaceShader.isCompiled()) {
            spriteBatch.setShader(spaceShader);
            Gdx.app.log(this.getClass().getSimpleName(), "shader compiled successfully!");
        } else {
            Gdx.app.error(this.getClass().getSimpleName(), "shader failed to compile:\n" + spaceShader.getLog());
        }
    }
    
    @Override
    public void update(float delta) {
        cam.position.x = GameScreen.cam.position.x;
        cam.position.y = GameScreen.cam.position.y;
        
        // load and unload tiles
        updateTiles();
        
        float invert = GameScreen.isHyper() ? 1 : 0;
        float blend = 0f;
        CameraSystem cam = getEngine().getSystem(CameraSystem.class);
        blend = cam.getZoomLevel() / cam.getMaxZoomLevel();
        spaceShader.bind();
        spaceShader.setUniformf("u_blend", blend);
        spaceShader.setUniformf("u_invert", invert);
        
        spriteBatch.begin();
        drawParallaxTiles();
        spriteBatch.end();
    }
    
    private void drawParallaxTiles() {
        float centerScreenX = Gdx.graphics.getWidth() * 0.5f;
        float centerScreenY = Gdx.graphics.getHeight() * 0.5f;
        for (SpaceBackgroundTile tile : tiles) {
            float drawX = (tile.x - cam.position.x * tile.depth) + centerScreenX;
            float drawY = (tile.y - cam.position.y * tile.depth) + centerScreenY;
            
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
    
    private void updateTiles() {
        dustCenterTile = updateLayer(dustCenterTile, dustTileDepth, SpaceBackgroundTile.TileType.Dust);
        star0CenterTile = updateLayer(star0CenterTile, 0, SpaceBackgroundTile.TileType.Stars);
        star1CenterTile = updateLayer(star1CenterTile, star1TileDepth, SpaceBackgroundTile.TileType.Stars);
        star2CenterTile = updateLayer(star2CenterTile, star2TileDepth, SpaceBackgroundTile.TileType.Stars);
        star3CenterTile = updateLayer(star3CenterTile, star3TileDepth, SpaceBackgroundTile.TileType.Stars);
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
            lastTile = new Vector2(currentTile);
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
    private static Vector2 getTilePos(float posX, float posY, float depth) {
        float x = (posX + tileSize) * depth;
        float y = (posY + tileSize) * depth;
        
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
        //return tempVec.set(tX, tY);//todo: cache so we're not create 3 new vecs per frame
    }
    //static Vector2 tempVec = new Vector2();
    
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
                boolean isTileLoaded = false;
                for (int index = 0; index < tiles.size(); ++index) {
                    SpaceBackgroundTile t = tiles.get(index);
                    if (t.tileX == tX && t.tileY == tY && t.depth == depth && t.type == type) {
                        isTileLoaded = true;
                        break;
                    }
                }
                
                // create and add tile if doesn't exist
                if (!isTileLoaded) {
                    //Gdx.app.debug(this.getClass().getSimpleName(), "Load " + type + " tile: [" + depth + "]: " + tX + ", " + tY);
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
            if (tile.depth == depth && !tileIsNear(centerTile, tile)) {
                //Gdx.app.debug(this.getClass().getSimpleName(), "Unload " + tile.type + " tile: [" + depth + "]: " + tile.tileX + ", " + tile.tileY);
                
                // dispose the texture so it doesn't eat up memory
                tile.tex.dispose();
                
                // remove tile
                tiles.remove(index);
                
                // reset search index because removing elements changes position of elements
                index = -1;
            }
        }
    }
    
    /** Check if a tile is within range of center tile. */
    private boolean tileIsNear(Vector2 centerTile, SpaceBackgroundTile tileToCheck) {
        return Math.abs(tileToCheck.tileX - centerTile.x) <= surround &&
               Math.abs(tileToCheck.tileY - centerTile.y) <= surround;
    }
    
    @Override
    public void dispose() {
        //dispose of textures
        for (SpaceBackgroundTile t : tiles) {
            t.tex.dispose();
        }
        tiles.clear();
        
        spriteBatch.dispose();
        spaceShader.dispose();
    }
    
}
