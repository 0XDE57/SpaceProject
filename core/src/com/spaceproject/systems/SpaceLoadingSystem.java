package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.spaceproject.SpaceProject;
import com.spaceproject.Tile;
import com.spaceproject.components.BarycenterComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.noise.NoiseBuffer;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.generation.noise.NoiseThreadPoolExecutor;
import com.spaceproject.generation.noise.NoiseGenListener;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;
import com.spaceproject.generation.noise.NoiseThread;

import java.util.concurrent.LinkedBlockingQueue;


//region test
class AstroObject {
	int x, y;
	long seed;
	//BarycenterComponent.AstronomicalBodyType type;

	public AstroObject(Vector2 location) {
		x = (int)location.x;
		y = (int)location.y;
		seed = MyMath.getSeed(x, y);
/*
		switch (MathUtils.random(2)) {
			case 0: type = uniStellar; break;
			case 1: type = multiStellar; break;
			case 2: type = roguePlanet; break;
		}
*/
	}
}

class Universe {

	Array<Vector2> points;
	public Array<AstroObject> objects = new Array<AstroObject>();

	public Universe(Array<Vector2> points) {
		for (Vector2 p : points){
			objects.add(new AstroObject(p));
		}
		this.points = points;
		saveToJson();
	}



	public void saveToJson() {
		Json json = new Json();
		json.setUsePrototypes(false);

		System.out.println(json.toJson(this));

		FileHandle keyFile = Gdx.files.local("save/" +  this.getClass().getSimpleName() + ".json");
		try {
			keyFile.writeString(json.toJson(this), false);
		} catch (GdxRuntimeException ex) {
			System.out.println("Could not save file: " + ex.getMessage());
		}
	}
}
//endregion


public class SpaceLoadingSystem extends EntitySystem implements NoiseGenListener, EntityListener, Disposable {

	// star entities
	//private Array<Vector2> points = new Array<Vector2>();
	private Universe universe;
	private ImmutableArray<Entity> loadedAstronomicalObjects;
	private float checkStarsTimer = 4000;
	private float checkStarsCurrTime;

	LinkedBlockingQueue<NoiseBuffer> noiseBuffer = new LinkedBlockingQueue<NoiseBuffer>();
	NoiseThreadPoolExecutor noiseThreadPool;

	@Override
	public void addedToEngine(Engine engine) {

		// currently loaded stars/planets
		loadedAstronomicalObjects = engine.getEntitiesFor(Family.all(BarycenterComponent.class, TransformComponent.class).get());

		engine.addEntityListener(Family.one(PlanetComponent.class, /*StarComponent.class,*/ BarycenterComponent.class).get(),this);

		// generate or load points from disk
		//loadPoints();

		universe = new Universe(generatePoints());

		noiseThreadPool = new NoiseThreadPoolExecutor(SpaceProject.celestcfg.maxGenThreads);
		noiseThreadPool.addListener(this);

		// load planetary systems / planets / stars
		//updateStars(1);
		// load space things (asteroids, wormhole, black hole, etc)
		// load ai/mobs

	}


	@Override
	public void entityAdded(Entity entity) {
		PlanetComponent planet = Mappers.planet.get(entity);
		if (planet != null) {
			SeedComponent seedComp = Mappers.seed.get(entity);
			//noiseThreads.add(new NoiseThread(seedComp, planet, Tile.defaultTiles));
			//TODO: check noise map exists in universe file first, if so load into tileMap queue
			//if not do noise. perhaps add to some sort of noise queue?
			//
			noiseThreadPool.execute(new NoiseThread(seedComp, planet, Tile.defaultTiles));
		}
	}

	@Override
	public void entityRemoved(Entity entity) {
		/*
		for (Entity e : getEngine().getEntitiesFor(Family.all(OrbitComponent.class).get())){
			OrbitComponent orbit = Mappers.orbit.get(e);
			if (orbit.parent != null) {
				//TODO: check if parents have parents (eg: moon > planet > star)
				//TODO: dispose texture. research auto texture dispose -> entity.componentRemoved()?
				if (orbit.parent == star) {
					getEngine().removeEntity(e);
				}
			}
		}*/
	}

	@Override
	public void threadFinished(NoiseThread noise) {
		//TODO: save in universe file for caching and loading instantly when transition to world
		noiseBuffer.add(noise.getMap());
	}

	
	@Override
	public void update(float delta) {
		// load and unload stars
		updateStars(delta);

		// update/replace textures
		updatePlanetTextures();
	}


	private void updatePlanetTextures() {

		//check queue for tilemaps / pixmaps to load into textures
		if (!noiseBuffer.isEmpty()) {
			try {
				NoiseBuffer noise = noiseBuffer.take();
				if (noise.pixelatedTileMap == null) {
					System.out.println("ERROR, no map for: [" + noise.ID + "]");
					return;
				}

				for (Entity p : getEngine().getEntitiesFor(Family.all(PlanetComponent.class).get())) {
					//if (p.getComponent(SeedComponent.class).seed == noise.seed) {//base on seed instead of ID, but what if duplicate seed? shouldn't happen..
					if (p.getComponent(PlanetComponent.class).tempGenID == noise.ID) {
						// create planet texture from tileMap, replace texture
						Texture newTex = TextureFactory.generatePlanet(noise.pixelatedTileMap, Tile.defaultTiles);
						p.getComponent(TextureComponent.class).texture = newTex;
						System.out.println("Texture loaded: [" + noise.ID + "]");
						return;
					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


		/*
		if (noiseThreads.size == 0) {
			return;
		}
		
		//if a thread has finished generating the noise, create a texture and replace the blank one

		for (NoiseThread thread : noiseThreads) {
			if (thread.isDone() && !thread.isProcessed()) {			
				for (Entity p : getEngine().getEntitiesFor(Family.all(PlanetComponent.class).get())) {
					if (p.getComponent(PlanetComponent.class).tempGenID == thread.getID()) {
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

		}*/
		
	}


	private void updateStars(float delta) {
		//TODO: use SimpleTimer
		checkStarsCurrTime -= 1000 * delta;
		if (checkStarsCurrTime < 0) {
			checkStarsCurrTime = checkStarsTimer; // reset timer
			
			//distance to check when to load planets
			int loadDistance = (int) SpaceProject.celestcfg.loadSystemDistance;
			loadDistance *= loadDistance;//square for dist2
			
			// remove stars from engine that are too far
			for (Entity star : loadedAstronomicalObjects) {
				TransformComponent t = Mappers.transform.get(star);
				if (Vector2.dst2(t.pos.x, t.pos.y, GameScreen.cam.position.x, GameScreen.cam.position.y) > loadDistance) {
					for (Entity e : getEngine().getEntitiesFor(Family.all(OrbitComponent.class).get())){
						OrbitComponent orbit = Mappers.orbit.get(e);
						if (orbit.parent != null) {
							//TODO: check if parents have parents (eg: moon > planet > star)
							//TODO: dispose texture. research auto texture dispose -> entity.componentRemoved()?
							if (orbit.parent == star) {
								getEngine().removeEntity(e);
							}
						}
					}
					getEngine().removeEntity(star);
					System.out.println("Removed Planetary System: " + star.getComponent(TransformComponent.class).pos.toString());
				}
			}
			
			// add planetary systems to engine
			for (AstroObject point : universe.objects) {
				//check if point is close enough to be loaded
				if (new Vector2(point.x, point.y).dst2(GameScreen.cam.position.x, GameScreen.cam.position.y) < loadDistance) {
			
					// check if star is already in world
					//TODO: check based on an ID rather than distance. more reliable and makes more sense than a distance check
					boolean loaded = false;
					for (Entity astroEntity : loadedAstronomicalObjects) {
						SeedComponent s = Mappers.seed.get(astroEntity);
						loaded = (s.seed == point.seed);
						/*
						TransformComponent t = Mappers.transform.get(astroEntity);

						if (point.dst(t.pos.x, t.pos.y) < 2f) {
							loaded = true;
						}*/
					}
					
					if (!loaded) {
						for (Entity e : EntityFactory.createAstronomicalObjects(point.x, point.y)) {
							//add entity to world
							getEngine().addEntity(e);
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
		//Array<Vector2> points = generatePoints();// create points
		//points.add(new Vector2(1000, 1000));//debug, don't forget about me
		//System.out.println("[GENERATE DATA] : Created " + points.size + " points...");


		//TODO: replace with new save method in universe
		/*
		// create handle for file storing points
		FileHandle starsFile = Gdx.files.local("save/stars.txt");

		starsFile.delete();//debug, don't save for now

		if (starsFile.exists()) {
			loadPoints(starsFile);

		} else {
			points = generatePoints();// create points
			System.out.println("[GENERATE DATA] : Created " + points.size + " points...");
			savePoints(starsFile);
		}
		*/

		
	}

	/*
	private void savePoints(FileHandle starsFile) {

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

	private void loadPoints(FileHandle starsFile) {
		// load points
		try {
            for (String line : starsFile.readString().replaceAll("\\r", "").split("\\n")) {
                String[] coords = line.split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                points.add(new Vector2(x, y));
            }
            System.out.println("[LOAD DATA] : Loaded " + points.size + " points...");
        } catch (GdxRuntimeException ex) {
            System.out.println("Could not load file: " + ex.getMessage());
        }
	}*/

	/**
	 * Generate list of points for position of stars/planetary systems.
	 * 
	 * @return list of Vector2 representing points
	 */
	private static Array<Vector2> generatePoints() {
		MathUtils.random.setSeed(SpaceProject.SEED);
		Array<Vector2> points = new Array<Vector2>();
		
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
				for (int j = 0; j < points.size && !reGen; j++) {
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


		points.add(new Vector2(1000, 1000));//TODO: system near origin for debug, don't forget about me

		return points;
	}


	public Array<Vector2> getPoints() {
		return universe.points;
	}


	public void dispose() {
		noiseThreadPool.shutdown();
		/*
		// stop and clear threads
		for (NoiseThread thread : noiseThreads) {
			thread.stop();
		}
		noiseThreads.clear();*/
	}



}
