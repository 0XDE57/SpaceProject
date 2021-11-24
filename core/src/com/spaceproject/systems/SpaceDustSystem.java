package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.screens.GameScreen;

import java.util.ArrayList;

public class SpaceDustSystem extends EntitySystem implements Disposable {
    
    // TODO: performs horribly when hyper drive active due to rapid create and delete textures
    // should change to 1 single tile of 4d noise tile, so it wraps we can tile infinitely, no need to generate on fly
    // also then we can apply shader and shift Z to bring dust to life?!?
    
    class Tile {
        public Texture texture;
        public int x;
        public int y;
    
        public Tile(Texture texture, int tX, int tY) {
            this.texture = texture;
            x = tX;
            y = tY;
        }
    }
    
    private OrthographicCamera cam;
    private SpriteBatch spriteBatch;
    
    private final ArrayList<Tile> tiles = new ArrayList<>();;
    
    private Vector2 centerTile;
    
    private static final int tileSize = 256;
    private int surround = 2;// how many tiles to load around center tile
    
    private float zoomFade = 21;
    
    @Override
    public void addedToEngine(Engine engine) {
        cam = GameScreen.cam;
        spriteBatch = new SpriteBatch();
    }
    
    @Override
    public void update(float delta) {
        //fade out when zoom out
        float alpha = MathUtils.clamp(1 - (cam.zoom / zoomFade), 0, 1);
        if (cam.zoom == 1) {
            alpha = 1;
        }
        
        if (alpha != 0) {
            //update layer
            centerTile = updateLayer(centerTile);
    
            //render
            drawTiles(alpha);
        }
    }
    
    private void drawTiles(float alpha) {
        spriteBatch.setColor(1, 1, 1, alpha);
        
        //render
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        for (Tile tile : tiles) {
            float drawX = (tile.x * tileSize);
            float drawY = (tile.y * tileSize);
            float width = tile.texture.getWidth();
            float height = tile.texture.getHeight();
            
            spriteBatch.draw(tile.texture, drawX, drawY,
                    0, 0,
                    width, height,
                    1, 1,
                    0, 0, 0, (int) width, (int) height, false, false);
        }
        spriteBatch.end();
    }
    
    
    private Vector2 updateLayer(Vector2 lastTile) {
        //calculate tile camera is within
        Vector2 currentTile = getTilePos(cam.position.x, cam.position.y);

        //load initial tiles
        if (lastTile == null) {
            lastTile = new Vector2(currentTile);
            loadTiles(currentTile);
        }
        
        //check if moved tile
        if (currentTile.x != lastTile.x || currentTile.y != lastTile.y) {
            // unload old tiles
            unloadTiles(currentTile);
            
            // load new tiles
            loadTiles(currentTile);
        }
        
        return currentTile;
    }
    
    private static Vector2 getTilePos(float x, float y) {
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
    
    private void loadTiles(Vector2 centerTile) {
        for (int tX = (int) centerTile.x - surround; tX <= centerTile.x + surround; tX++) {
            for (int tY = (int) centerTile.y - surround; tY <= centerTile.y + surround; tY++) {
                // check if tile already exists
                boolean isTileLoaded = false;
                for (int index = 0; index < tiles.size(); ++index) {
                    Tile t = tiles.get(index);
                    if (t.x == tX && t.y == tY) {
                        isTileLoaded = true;
                        break;
                    }
                }
                
                // create and add tile if doesn't exist
                if (!isTileLoaded) {
                    tiles.add(new Tile(TextureFactory.generateSpaceDust(tX, tY, tileSize), tX, tY));
                }
            }
        }
    }
    
    private void unloadTiles(Vector2 centerTile) {
        for (int index = 0; index < tiles.size(); ++index) {
            Tile tile = tiles.get(index);
            if (!tileIsNear(centerTile, tile)) {
                // dispose the texture so it doesn't eat up memory
                tile.texture.dispose();
                
                // remove tile
                tiles.remove(index);
                
                // reset search index because element was removed
                index = -1;
            }
        }
    }
    
    /** Check if a tile is within range of center tile. */
    private boolean tileIsNear(Vector2 centerTile, Tile tileToCheck) {
        return Math.abs(tileToCheck.x - centerTile.x) <= surround &&
               Math.abs(tileToCheck.y - centerTile.y) <= surround;
    }
    
    @Override
    public void dispose() {
        //dispose of textures
        for (Tile t : tiles) {
            t.texture.dispose();
        }
        tiles.clear();
    }
    
}
