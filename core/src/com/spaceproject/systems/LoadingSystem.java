package com.spaceproject.systems;

import java.util.ArrayList;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceBackgroundTile;
import com.spaceproject.components.TransformComponent;

public class LoadingSystem extends IteratingSystem {
	
	//background layer of tiles
	private static ArrayList<SpaceBackgroundTile> bgTileLayer = new ArrayList<SpaceBackgroundTile>();
	private static float bgTileDepth = 0.9f; //multiplier for position of tile
	
	//foreground layer of tiles (not implemented yet)
	//private ArrayList<SpaceBackgroundTile> fgTileLayer = new ArrayList<SpaceBackgroundTile>();	
	//private static float fgTileDepth = 0.7f;
	
	private static int tileSize = 1024; //how large a tile texture is
	private int surround = 1;//how many tiles to load around center tile
	private Vector2 centerTile; //center tile to check for tile change
	
	//timer for how often to check if player moved tiles
	private float checkTileTimer = 500; 
	private float checkTileCurrTime = checkTileTimer;
	
	public Entity playerEntity = null; //the player entity target reference
	
	private ComponentMapper<TransformComponent> transformMap;
	
	
	public LoadingSystem(Entity player) {
		super(Family.all(TransformComponent.class).get());
		transformMap = ComponentMapper.getFor(TransformComponent.class);
		
		//target for tiles
		this.playerEntity = player;
		
		// load initial tiles
		TransformComponent pos = transformMap.get(playerEntity);
		centerTile = getTilePos(pos.pos.x, pos.pos.y);
		loadTiles(centerTile);

		
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
	public static Vector2 getTilePos(float posX, float posY) {	
		//calculate position
		int x = (int) (posX - (RenderingSystem.getCam().position.x - (tileSize/2)) * bgTileDepth);
		int y = (int) (posY - (RenderingSystem.getCam().position.y - (tileSize/2)) * bgTileDepth);
		
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
		
		
		checkTileCurrTime -= 1000 * delta;
		if (checkTileCurrTime < 0) {

			// get tile player is in
			TransformComponent pos = transformMap.get(playerEntity);
			Vector2 newTile = getTilePos(pos.pos.x, pos.pos.y);
						
			// check if player has changed tiles
			if (newTile.x != centerTile.x || newTile.y != centerTile.y) {
				
				//unload old tiles
				unloadTiles(newTile);

				//load new tiles
				loadTiles(newTile);

			}
		
			//set new centerTile
			centerTile = newTile;
			
			// reset timer
			checkTileCurrTime = checkTileTimer;
		}

	}

	/** 
	 * Load tiles surrounding centerTile.
	 * @param centerTile 
	 */
	private void loadTiles(Vector2 centerTile) {

		for (int tX = (int) centerTile.x - surround; tX <= centerTile.x + surround; tX++) {
			for (int tY = (int) centerTile.y - surround; tY <= centerTile.y + surround; tY++) {

				// check if tile already exists
				boolean exists = false;
				for (int index = 0; index < bgTileLayer.size() && !exists; ++index) {
					if (bgTileLayer.get(index).tileX == tX
							&& bgTileLayer.get(index).tileY == tY) {
						exists = true;
					}
				}
				
				// create and add tile if doesn't exist
				if (!exists) {
					bgTileLayer.add(new SpaceBackgroundTile(tX, tY, bgTileDepth, tileSize));
				}
			}
		}
	}

	/** 
	 * Remove any tiles not surrounding centerTile.
	 * @param centerTile
	 */
	private void unloadTiles(Vector2 centerTile) {
		
		for (int index = 0; index < bgTileLayer.size(); ++index) {

			if (tileIsNear(centerTile, bgTileLayer.get(index))) {

				// dispose the texture so it doesn't eat up memory
				bgTileLayer.get(index).tex.dispose();
				// remove tile
				bgTileLayer.remove(index);

				// reset index because removing elements changes
				// elements position in array
				index = -1;
				if (index >= bgTileLayer.size()) {
					continue;
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
	
	public static ArrayList<SpaceBackgroundTile> getFGTileLayer() {
		return bgTileLayer;
	}
}
