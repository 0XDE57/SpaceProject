package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.SpaceBackgroundTile;
import com.spaceproject.ui.SpaceBackgroundTile.TileType;

import java.util.ArrayList;

public class SpaceParallaxSystem extends EntitySystem implements Disposable {
    
    private final SpriteBatch spriteBatch;
    private final Matrix4 projectionMatrix = new Matrix4();
    
    // background layer of tiles
    private final ArrayList<SpaceBackgroundTile> tiles = new ArrayList<>();
    
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
    
    //todo: split surround into separate x/y, calculate tiles needed:
    // tX = GDX.graphics.getWidth()/tileSize;
    // tY = GDX.graphics.getHeight()/tileSize;
    // load appropriate amount of tiles for any resolution
    // or fixed size then scaled to extended viewport?
    private static final int tileSize = 512;//1024;//
    private int surroundX, surroundY;// how many tiles to load around center tile
    private final ShaderProgram spaceShader;
    
    
    public SpaceParallaxSystem() {
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
        // load and unload tiles
        updateTiles();
        
        float invert = GameScreen.isHyper() ? 1 : 0;
        CameraSystem cam = getEngine().getSystem(CameraSystem.class);
        float blend = cam.getZoomLevel() / cam.getMaxZoomLevel();
        spaceShader.bind();
        spaceShader.setUniformf("u_blend", blend);
        spaceShader.setUniformf("u_invert", invert);
        
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        spriteBatch.setProjectionMatrix(projectionMatrix);
        spriteBatch.begin();
        drawParallaxTiles();
        spriteBatch.end();
    }
    
    private void drawParallaxTiles() {
        float centerScreenX = Gdx.graphics.getWidth() * 0.5f;
        float centerScreenY = Gdx.graphics.getHeight() * 0.5f;
        for (SpaceBackgroundTile tile : tiles) {
            float centerTile = tileSize * 0.5f;
            float drawX = ((-GameScreen.cam.position.x * tile.depth) + tile.x - centerTile) + centerScreenX;
            float drawY = ((-GameScreen.cam.position.y * tile.depth) + tile.y - centerTile) + centerScreenY;
            float width = tile.tex.getWidth();
            float height = tile.tex.getHeight();
            //draw texture
            spriteBatch.draw(tile.tex, drawX, drawY,
                    0, 0,
                    width, height,
                    tile.scale, tile.scale,
                    0, 0, 0, (int) width, (int) height, false, false);
        }
    }
    
    private void updateTiles() {
        int prevX = surroundX;
        int prevY = surroundY;
        surroundX = Gdx.graphics.getWidth() / tileSize;
        surroundY = Gdx.graphics.getHeight() / tileSize;
        boolean resize = (prevX != surroundX || prevY != surroundY);
        dustCenterTile = updateLayer(resize, dustCenterTile, dustTileDepth, SpaceBackgroundTile.TileType.Dust);
        star0CenterTile = updateLayer(resize, star0CenterTile, 0, SpaceBackgroundTile.TileType.Stars);
        star1CenterTile = updateLayer(resize, star1CenterTile, star1TileDepth, SpaceBackgroundTile.TileType.Stars);
        star2CenterTile = updateLayer(resize, star2CenterTile, star2TileDepth, SpaceBackgroundTile.TileType.Stars);
        star3CenterTile = updateLayer(resize, star3CenterTile, star3TileDepth, SpaceBackgroundTile.TileType.Stars);
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
    private Vector2 updateLayer(boolean resize, Vector2 lastTile, float depth, SpaceBackgroundTile.TileType type) {
        //calculate tile camera is within
        Vector2 currentTile = getTilePos(GameScreen.cam.position.x, GameScreen.cam.position.y, depth);

        //load initial tiles
        if (lastTile == null) {
            lastTile = new Vector2(currentTile);
            loadTiles(currentTile, depth, type);
        }
        
        //check if moved tile
        if (currentTile.x != lastTile.x || currentTile.y != lastTile.y || resize) {
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
        float x = (posX * depth) + (tileSize * 0.5f);
        float y = (posY * depth) + (tileSize * 0.5f);
        
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
        for (int tX = (int) centerTile.x - surroundX; tX <= centerTile.x + surroundX; tX++) {
            for (int tY = (int) centerTile.y - surroundY; tY <= centerTile.y + surroundY; tY++) {
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
                    //Gdx.app.debug(this.getClass().getSimpleName(), "Load " + type + " tile: [" + depth + "]: " + tX + ", " + tY
                    //    + " surround: " + surroundX + ", " + surroundY);
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
        return Math.abs(tileToCheck.tileX - centerTile.x) <= surroundX &&
               Math.abs(tileToCheck.tileY - centerTile.y) <= surroundY;
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
