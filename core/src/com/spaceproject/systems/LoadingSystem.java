package com.spaceproject.systems;

import java.util.ArrayList;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceBackgroundTile;
import com.spaceproject.components.PlayerFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class LoadingSystem extends IteratingSystem {
	
	//background layer of tiles
	private static ArrayList<SpaceBackgroundTile> tiles = new ArrayList<SpaceBackgroundTile>();
	
	//multiplier for position of tile
	private static float bgTileDepth = 0.9f; //background
	private static float fgTileDepth = 0.8f; //foreground
	
	//center tile to check for tile change
	private Vector2 bgCenterTile; //background
	private Vector2 fgCenterTile; //foreground	
		
	private static int tileSize = 1024; //how large a tile texture is
	private int surround = 1;//how many tiles to load around center tile
		
	//timer for how often to check if player moved tiles
	private float checkTileTimer = 500; 
	private float checkTileCurrTime = checkTileTimer;
	
	public LoadingSystem() {
		super(Family.all(PlayerFocusComponent.class).get());
		
		//load tiles
		//load spacedust/background clouds(noise/fractals)
		//load planetary systems / planets / stars
		//load space things (asteroids, wormhole, black hole, etc)
		//load ai/mobs
	}
	
	/** Convert world position to tile position.  
	 * @param posX
	 * @param posY
	 * @return tile that an object is in.
	 */
	public static Vector2 getTilePos(float posX, float posY, float depth) {	
		//calculate position
		int x = (int) (posX - (RenderingSystem.getCam().position.x - (tileSize/2)) * depth);
		int y = (int) (posY - (RenderingSystem.getCam().position.y - (tileSize/2)) * depth);
		
		//calculate tile that position is in
		int tX = x / tileSize;
		int tY = y / tileSize;
		
		//subtract 1 from tile position if less than zero to account for -1/x giving 0
		if (x < 0) {
			--tX;
		}
		if (y < 0) {
			--tY;
		}	
		
		return new Vector2(tX, tY);
	}

	@Override
	protected void processEntity(Entity entity, float delta) {
		// TODO: consider adding timers to break up the process from happening
		// in one frame causing a freeze/jump
		// because putting it in a separate thread is not possible due to
		// glContext...	
		
		// Also: consider refactoring to allow for arbitrary number layers(depths)
		
		checkTileCurrTime -= 1000 * delta;
		if (checkTileCurrTime < 0) {

			// get tiles player is in
			TransformComponent pos = Mappers.transform.get(entity);
			Vector2 bgTile = getTilePos(pos.pos.x, pos.pos.y, bgTileDepth);
			Vector2 fgTile = getTilePos(pos.pos.x, pos.pos.y, fgTileDepth);
			
			if (bgCenterTile == null) {
				bgCenterTile = bgTile;
				loadTiles(bgTile, bgTileDepth);
			}
			
			if (fgCenterTile == null) {
				fgCenterTile = fgTile;
				loadTiles(fgTile, fgTileDepth);
			}
			
			// check if player has changed background tiles
			if (bgTile.x != bgCenterTile.x || bgTile.y != bgCenterTile.y) {

				// unload old tiles
				unloadTiles(bgTile, bgTileDepth);

				// load new tiles
				loadTiles(bgTile, bgTileDepth);
				
				//store tile
				bgCenterTile = bgTile;
			}
			
			// check if player has changed foreground tiles
			if (fgTile.x != fgCenterTile.x || fgTile.y != fgCenterTile.y) {
				
				//unload old tiles
				unloadTiles(fgTile, fgTileDepth);

				//load new tiles
				loadTiles(fgTile, fgTileDepth);

				//store tile
				fgCenterTile = fgTile;
			}
					
			// reset timer
			checkTileCurrTime = checkTileTimer;
		}

	}

	/**
	 * Load tiles surrounding centerTile of specified depth.
	 * @param centerTile
	 * @param depth
	 */
	private void loadTiles(Vector2 centerTile, float depth) {

		for (int tX = (int) centerTile.x - surround; tX <= centerTile.x + surround; tX++) {
			for (int tY = (int) centerTile.y - surround; tY <= centerTile.y + surround; tY++) {
				// check if tile already exists
				boolean exists = false;
				for (int index = 0; index < tiles.size() && !exists; ++index) {
					SpaceBackgroundTile t = tiles.get(index);
					if (t.tileX == tX && t.tileY == tY && t.depth == depth) {
						exists = true;
					}
				}
				
				// create and add tile if doesn't exist
				if (!exists) {
					tiles.add(new SpaceBackgroundTile(tX, tY, depth, tileSize));
					//bgTileLayer.add(new SpaceBackgroundTile(tX, tY, fgTileDepth, tileSize));
				}
			}
		}
	}

	/** 
	 * Remove any tiles not surrounding centerTile of same depth.
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
	 *  Check if a tile is within range of center tile.
	 */
	private boolean tileIsNear(Vector2 centerTile, SpaceBackgroundTile tileToCheck) {
		return tileToCheck.tileX < centerTile.x - surround
		 || tileToCheck.tileX > centerTile.x + surround
		 || tileToCheck.tileY < centerTile.y - surround
		 || tileToCheck.tileY > centerTile.y + surround;
	}
	
	public static ArrayList<SpaceBackgroundTile> getTiles() {
		return tiles;
	}
}
