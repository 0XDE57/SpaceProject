package com.spaceproject.systems;

import java.util.ArrayList;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.spaceproject.SpaceBackgroundTile;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.StarComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.utility.Mappers;

public class LoadingSystem extends EntitySystem {

	private Engine engine;

	// background layer of tiles
	private static ArrayList<SpaceBackgroundTile> tiles = new ArrayList<SpaceBackgroundTile>();

	// multiplier for position of tile
	private static float bgTileDepth = 0.9f; // background
	private static float fgTileDepth = 0.8f; // foreground

	// center tile to check for tile change
	private Vector2 bgCenterTile; // background
	private Vector2 fgCenterTile; // foreground

	private static int tileSize = 1024; // how large a tile texture is
	private int surround = 1;// how many tiles to load around center tile

	// timer for how often to check if player moved tiles
	private float checkTileTimer = 500;
	private float checkTileCurrTime = checkTileTimer;

	private ArrayList<Vector2> points = new ArrayList<Vector2>();
	private ImmutableArray<Entity> loadedStars;
	private float checkStarsTimer = 4000;
	private float checkStarsCurrTime = checkStarsTimer;

	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;

		// currently loaded stars/planets
		loadedStars = engine.getEntitiesFor(Family.all(StarComponent.class, TransformComponent.class).get());

		// generate or load points from disk
		loadStars();

		
		////////////////////TEST/////////////////////
		for (Vector2 point : points) {
			System.out.println(point);
			/*
			for (Entity e : EntityFactory.createPlanetarySystem(point.x, point.y)) {
				engine.addEntity(e);
			}*/
		}
		//////////////////////////////////////////*/

		// load tiles
		// load spacedust/background clouds(noise/fractals)
		// load planetary systems / planets / stars
		// load space things (asteroids, wormhole, black hole, etc)
		// load ai/mobs
	}

	@Override
	public void update(float delta) {

		// position of camera to know where to load
		Vector3 camPos = RenderingSystem.getCamPos();

		// check, load and unload tiles
		updateTiles(delta, camPos);

		// check, load and unload stars
		updateStars(delta, camPos);

	}

	/**
	 * If camera has changed position relative to tiles, unload far tiles and
	 * load near tiles.
	 * 
	 * @param delta
	 * @param camPos
	 */
	private void updateTiles(float delta, Vector3 camPos) {
		// TODO: consider adding timers to break up the process from happening
		// in one frame causing a freeze/jump
		// because putting it in a separate thread is not working (possible?)
		// due to
		// glContext...

		// Also: consider refactoring to allow for arbitrary number
		// layers(depths)

		// timer to check when player has changed tiles
		checkTileCurrTime -= 1000 * delta;
		if (checkTileCurrTime < 0) {

			// get tiles camera is in
			Vector2 bgTile = getTilePos(camPos.x, camPos.y, bgTileDepth);
			Vector2 fgTile = getTilePos(camPos.x, camPos.y, fgTileDepth);

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

				// store tile
				bgCenterTile = bgTile;
			}

			// check if player has changed foreground tiles
			if (fgTile.x != fgCenterTile.x || fgTile.y != fgCenterTile.y) {

				// unload old tiles
				unloadTiles(fgTile, fgTileDepth);

				// load new tiles
				loadTiles(fgTile, fgTileDepth);

				// store tile
				fgCenterTile = fgTile;
			}

			// reset timer
			checkTileCurrTime = checkTileTimer;
		}
	}


	private void updateStars(float delta, Vector3 camPos) {
		checkStarsCurrTime -= 1000 * delta;
		if (checkStarsCurrTime < 0) {
			System.out.println("Checking stars...");
			
			// same as planetarySystem values from entityfactory
			// TODO: load these values from a file to make sure they are consistent
			int maxPlanets = 10;
			float maxDist = 2200;
			int loadDistance = (int) (maxPlanets*maxDist*3f);
			loadDistance *= loadDistance; // squared for quick distance checking

			// remove stars from engine that are too far		
			for (Entity star : loadedStars) {
				TransformComponent t = Mappers.transform.get(star);
				if (Vector2.dst2(t.pos.x, t.pos.y, camPos.x, camPos.y) > loadDistance) {
					for (Entity e : engine.getEntitiesFor(Family.all(OrbitComponent.class).get())){
						OrbitComponent orbit = Mappers.orbit.get(e);
						if (orbit.parent != null) {
							//TODO: check if parents have parents (eg: moon > planet > star)
							if (orbit.parent == star) {
								engine.removeEntity(e);
							}
						}
					}
					engine.removeEntity(star);
				}
			}
			
			// add planetary systems to engine
			for (Vector2 point : points) {
				//check if point is close enough to be loaded
				if (point.dst2(camPos.x, camPos.y) < loadDistance) {

					boolean loaded = false;
					
					// check if star is already in world
					for (Entity star : loadedStars) {
						TransformComponent t = Mappers.transform.get(star);
						if (point.dst(t.pos.x, t.pos.y) < 1f) {
							loaded = true;
						}
					}
					
					if (!loaded) {
						//create new system
						for (Entity e : EntityFactory.createPlanetarySystem(point.x, point.y)) {
							engine.addEntity(e);
						}
					}

				}
			}

			// reset timer
			checkStarsCurrTime = checkStarsTimer;
		}
	}

	/**
	 * Fill world with stars and planets. Load points from disk or if no points
	 * exist, create points and save to disk.
	 */
	private void loadStars() {
		// TODO: only load the closest few systems surrounding player,
		// don't load all of them in the engine

		// create handle for file storing points
		FileHandle starsFile = Gdx.files.local("stars.txt");

		// starsFile.delete();

		if (starsFile.exists()) {
			// load points
			try {
				System.out.println("[LOAD DATA] : Loading points from disk...");
				for (String line : starsFile.readString().replaceAll("\\r", "").split("\\n")) {
					String[] coords = line.split(",");
					int x = Integer.parseInt(coords[0]);
					int y = Integer.parseInt(coords[1]);
					points.add(new Vector2(x, y));
				}
				System.out.println("[LOAD DATA] : Loaded " + points.size() + " points...");
			} catch (GdxRuntimeException ex) {
				System.out.println("Could not load file: " + ex.getMessage());
			}
		} else {
			System.out.println("[GENERATE DATA] : Generating points...");
			points = generatePoints();// create points
			System.out.println("[GENERATE DATA] : Created " + points.size() + " points...");
			try {
				System.out.println("[SAVE DATA] : Saving points to disk...");
				// save points to disk
				for (Vector2 p : points) {
					starsFile.writeString((int) p.x + "," + (int) p.y + "\n", true);
				}
				System.out.println("[SAVE DATA] : Points saved to: " + Gdx.files.getLocalStoragePath() + starsFile.path());
			} catch (GdxRuntimeException ex) {
				System.out.println("Could not save file: " + ex.getMessage());
			}
		}

	}

	/**
	 * Generate list of points for position of stars/planetary systems.
	 * 
	 * @return list of Vector2 representing points
	 */
	private ArrayList<Vector2> generatePoints() {
		MathUtils.random.setSeed(SpaceProject.SEED);
		ArrayList<Vector2> points = new ArrayList<Vector2>();
		int numStars = 150; // how many stars TRY to create(does not guarantee this many points will actually be generated)
		int genRange = 400000; // range from origin(0,0) to create points

		// same as planetarySystem values from entityfactory
		// TODO: load these values from a file to make sure they are consistent
		int maxPlanets = 10;
		float maxDist = 2200;
		// maxplanets*maxdistance*3
		float dist = maxPlanets * maxDist * 6; // minimum distance between points
		dist *= dist;// squared for quick distance checking

		// generate points
		for (int i = 0; i < numStars; i++) {
			Vector2 newPoint;

			boolean reGen = false; // flag for if the point is needs to be
									// regenerated or not
			boolean fail = false; // flag for when to give up generating a point
			int fails = 0; // how many times a point has been regenerated
			do {
				// create point at random position
				int x = MathUtils.random(-genRange, genRange);
				int y = MathUtils.random(-genRange, genRange);
				newPoint = new Vector2(x, y);

				// check for collisions
				reGen = false;
				for (int j = 0; j < points.size() && !reGen; j++) {
					// if point is too close to other point; regenerate
					if (newPoint.dst2(points.get(j)) <= dist) {
						reGen = true;

						// if too many tries, give up to avoid infinite or
						// exceptionally long loops
						fails++;
						if (fails > 3) {
							fail = true;
						}
					}
				}
			} while (reGen && !fail);

			// add point if valid
			if (!fail)
				points.add(newPoint);
		}

		return points;
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
		int x = (int) (posX - (RenderingSystem.getCamPos().x - (tileSize / 2)) * depth);
		int y = (int) (posY - (RenderingSystem.getCamPos().y - (tileSize / 2)) * depth);

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
					// bgTileLayer.add(new SpaceBackgroundTile(tX, tY,
					// fgTileDepth, tileSize));
				}
			}
		}
		System.out.println("Load tile: [" + depth + "]: " + centerTile.x + ", " + centerTile.y);
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
}
