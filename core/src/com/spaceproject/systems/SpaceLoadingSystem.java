package com.spaceproject.systems;

import java.util.ArrayList;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.spaceproject.SpaceBackgroundTile;
import com.spaceproject.SpaceBackgroundTile.TileType;
import com.spaceproject.SpaceProject;
import com.spaceproject.Tile;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.StarComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyScreenAdapter;
import com.spaceproject.utility.NoiseThread;

public class SpaceLoadingSystem extends EntitySystem implements Disposable {

	private Engine engine;
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
	private float checkTileTimer = 500;
	private float checkTileCurrTime;

	// star entities
	private ArrayList<Vector2> points;
	private ImmutableArray<Entity> loadedStars;
	private float checkStarsTimer = 4000;
	private float checkStarsCurrTime;
	
	//threads for generating planet texture noise
	ArrayList<NoiseThread> noiseThreads;

	public SpaceLoadingSystem() {
		this(MyScreenAdapter.cam);
	}
	
	public SpaceLoadingSystem(OrthographicCamera camera) {
		cam = camera;
		
		tiles = new ArrayList<SpaceBackgroundTile>();
		points = new ArrayList<Vector2>();
		noiseThreads = new ArrayList<NoiseThread>();
	}

	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;

		// currently loaded stars/planets
		loadedStars = engine.getEntitiesFor(Family.all(StarComponent.class, TransformComponent.class).get());

		// generate or load points from disk
		loadPoints();

		
		/*///////////////////TEST/////////////////////
		for (Vector2 point : points) {
			System.out.println(point);
			
			for (Entity e : EntityFactory.createPlanetarySystem(point.x, point.y)) {
				//engine.addEntity(e);
			}
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

		// load and unload tiles
		updateTiles(delta);

		// load and unload stars
		updateStars(delta);
		
		// noise generation threads, update/replace textures
		updatePlanetTextures();

	}

	private void updatePlanetTextures() {
		if (noiseThreads.isEmpty()) {
			return;
		}
		
		//if a thread has finished generating the noise, create a texture and replace the blank one
		for (NoiseThread thread : noiseThreads) {
			if (thread.isDone() && !thread.isProcessed()) {			
				for (Entity p : engine.getEntitiesFor(Family.all(PlanetComponent.class).get())) {
					if (p.getComponent(PlanetComponent.class).id == thread.getID()) {					
						int[][] tileMap = thread.getPixelatedMap();
						if (tileMap != null) {
							// create planet texture from tileMap, replace texture
							Texture newTex = TextureFactory.generatePlanet(tileMap, Tile.defaultTiles);
							p.getComponent(TextureComponent.class).texture = newTex;
							
							thread.setProcessed();//set flag to signify we are done with the thread
							System.out.println("Texture loaded: [" + thread.getID() + "]");
						}
					}
				}
			}
		}
		
		//remove completed threads
		//noiseThreads.removeIf(o -> o.isProcessed());//java8 lamba only supported in androidN+
		boolean finished = true;
		for (NoiseThread thread : noiseThreads) {
			if (!thread.isProcessed())
				finished = false;
		}
		if (finished) {
			noiseThreads.clear();
			System.out.println("All Planet Textures Loaded.");
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

		// TODO: refactor to simplify layers(depths)

		// timer to check when player has changed tiles
		checkTileCurrTime -= 1000 * delta;
		if (checkTileCurrTime < 0) {
			// reset timer
			checkTileCurrTime = checkTileTimer;

			// get tiles camera is in
			Vector2 dustTile = getTilePos(cam.position.x, cam.position.y, dustTileDepth);
			Vector2 bgTile = getTilePos(cam.position.x, cam.position.y, bgTileDepth);
			Vector2 fgTile = getTilePos(cam.position.x, cam.position.y, fgTileDepth);

			if (dustCenterTile == null) {
				dustCenterTile = dustTile;
				loadTiles(dustTile, dustTileDepth, SpaceBackgroundTile.TileType.Dust);
			}
			
			if (bgCenterTile == null) {
				bgCenterTile = bgTile;
				loadTiles(bgTile, bgTileDepth, SpaceBackgroundTile.TileType.Stars);
			}

			if (fgCenterTile == null) {
				fgCenterTile = fgTile;
				loadTiles(fgTile, fgTileDepth, SpaceBackgroundTile.TileType.Stars);
			}
			
			
			if (dustTile.x != dustCenterTile.x || dustTile.y != dustCenterTile.y) {

				// unload old tiles
				unloadTiles(dustTile, dustTileDepth);

				// load new tiles
				loadTiles(dustTile, dustTileDepth, SpaceBackgroundTile.TileType.Dust);

				// store tile
				dustCenterTile = dustTile;
			}

			// check if player has changed background tiles
			if (bgTile.x != bgCenterTile.x || bgTile.y != bgCenterTile.y) {

				// unload old tiles
				unloadTiles(bgTile, bgTileDepth);

				// load new tiles
				loadTiles(bgTile, bgTileDepth, SpaceBackgroundTile.TileType.Stars);

				// store tile
				bgCenterTile = bgTile;
			}

			// check if player has changed foreground tiles
			if (fgTile.x != fgCenterTile.x || fgTile.y != fgCenterTile.y) {

				// unload old tiles
				unloadTiles(fgTile, fgTileDepth);

				// load new tiles
				loadTiles(fgTile, fgTileDepth, SpaceBackgroundTile.TileType.Stars);

				// store tile
				fgCenterTile = fgTile;
			}

		}
	}

	private void updateStars(float delta) {
		checkStarsCurrTime -= 1000 * delta;
		if (checkStarsCurrTime < 0) {
			checkStarsCurrTime = checkStarsTimer; // reset timer
			
			//distance to check when to load planets
			int loadDistance = (int) SpaceProject.celestcfg.loadSystemDistance;
			loadDistance *= loadDistance;//square for dist2
			
			// remove stars from engine that are too far
			for (Entity star : loadedStars) {
				TransformComponent t = Mappers.transform.get(star);
				if (Vector2.dst2(t.pos.x, t.pos.y, cam.position.x, cam.position.y) > loadDistance) {
					for (Entity e : engine.getEntitiesFor(Family.all(OrbitComponent.class).get())){
						OrbitComponent orbit = Mappers.orbit.get(e);
						if (orbit.parent != null) {
							//TODO: check if parents have parents (eg: moon > planet > star)
							//TODO: dispose texture. research auto texture dispose -> entity.componentRemoved()?
							if (orbit.parent == star) {
								engine.removeEntity(e);
							}
						}
					}
					engine.removeEntity(star);
					System.out.println("Removed: " + star.getComponent(TransformComponent.class).pos.toString());
				}
			}
			
			// add planetary systems to engine
			for (Vector2 point : points) {
				//check if point is close enough to be loaded
				if (point.dst2(cam.position.x, cam.position.y) < loadDistance) {
			
					// check if star is already in world
					//TODO: check based on an ID rather than distance. more reliable and makes more sense than a distance check
					boolean loaded = false;
					for (Entity star : loadedStars) {
						TransformComponent t = Mappers.transform.get(star);
						if (point.dst(t.pos.x, t.pos.y) < 2f) {
							loaded = true;
						}
					}
					
					if (!loaded) {
						//create new system
						for (Entity e : EntityFactory.createPlanetarySystem(point.x, point.y)) {
							//add entity to world
							engine.addEntity(e);
														
							PlanetComponent planet = Mappers.planet.get(e);
							if (planet != null) {
								noiseThreads.add(new NoiseThread(planet, Tile.defaultTiles));
							}
						}
					}

				}
			}	
		}
	}
	
	/**
	 * Fill universe with stars and planets. Load points from disk or if no points
	 * exist, create points and save to disk.
	 */
	private void loadPoints() {
		points.clear();
		
		// create handle for file storing points
		FileHandle starsFile = Gdx.files.local("save/stars.txt");
		
		starsFile.delete();//debug, don't save for now
		
		if (starsFile.exists()) {
			// load points
			try {
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
			points = generatePoints();// create points
			System.out.println("[GENERATE DATA] : Created " + points.size() + " points...");
			try {
				// save points to disk
				for (Vector2 p : points) {
					starsFile.writeString((int) p.x + "," + (int) p.y + "\n", true);
				}
				System.out.println("[SAVE DATA] : Points saved to: " + Gdx.files.getLocalStoragePath() + starsFile.path());
			} catch (GdxRuntimeException ex) {
				System.out.println("Could not save file: " + ex.getMessage());
			}
		}
		
		//debug
		points.add(new Vector2(700, 700));//system near origin for debug

	}

	/**
	 * Generate list of points for position of stars/planetary systems.
	 * 
	 * @return list of Vector2 representing points
	 */
	private static ArrayList<Vector2> generatePoints() {
		MathUtils.random.setSeed(SpaceProject.SEED);
		ArrayList<Vector2> points = new ArrayList<Vector2>();
		
		// how many stars TRY to create(does not guarantee this many points will actually be generated)
		int numStars = SpaceProject.celestcfg.numPoints;
		// range from origin(0,0) to create points
		int genRange = SpaceProject.celestcfg.pointGenRange;
		// minimum distance between points
		float dist = SpaceProject.celestcfg.minPointDistance; 
		dist *= dist;//squared for dst2

		// generate points
		for (int i = 0; i < numStars; i++) {
			Vector2 newPoint;

			boolean reGen = false; // flag for if the point is needs to be regenerated
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
		System.out.println("Load " + type + " tile: [" + depth + "]: " + (int)centerTile.x + ", " + (int)centerTile.y);
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
		
		//stop and clear threads
		for (NoiseThread thread : noiseThreads) {
			thread.stop();
		}
		noiseThreads.clear();
		
	}
}
