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
	
	// star entities
	private ArrayList<Vector2> points;
	private ImmutableArray<Entity> loadedStars;
	private float checkStarsTimer = 4000;
	private float checkStarsCurrTime;
	
	// threads for generating planet texture noise
	ArrayList<NoiseThread> noiseThreads;

	public SpaceLoadingSystem() {
		this(MyScreenAdapter.cam);
	}
	
	public SpaceLoadingSystem(OrthographicCamera camera) {
		cam = camera;
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
		
		// load planetary systems / planets / stars
		// load space things (asteroids, wormhole, black hole, etc)
		// load ai/mobs

	}
	
	@Override
	public void update(float delta) {
		// load and unload stars
		updateStars(delta);

		// noise generation threads, update/replace textures
		updatePlanetTextures();
	}

	/**
	 * Handles noise generating threads. If a thread is finished generating noise, a texture is created
	 * from the noise and the planets texture is replaced with the new generated one.
	 * Once a texture has been replaced, the thread is then removed from the list and considered processed.
	 */
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

	@Override
	public void dispose() {
		// stop and clear threads
		for (NoiseThread thread : noiseThreads) {
			thread.stop();
		}
		noiseThreads.clear();
	}


}
