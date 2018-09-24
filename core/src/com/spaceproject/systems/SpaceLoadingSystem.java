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
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.generation.noise.NoiseBuffer;
import com.spaceproject.generation.noise.NoiseGenListener;
import com.spaceproject.generation.noise.NoiseThread;
import com.spaceproject.generation.noise.NoiseThreadPoolExecutor;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.SimpleTimer;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;


//region test
class AstroBody {
	int x, y;
	long seed;

	//BarycenterComponent.AstronomicalBodyType type;

	public AstroBody(Vector2 location) {
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

class Universe implements NoiseGenListener {

	Array<Vector2> points;
	public Array<AstroBody> objects = new Array<AstroBody>();
	public HashMap<Long, NoiseBuffer> loadedNoise = new HashMap<Long, NoiseBuffer>();

	public Universe() {
		this(generatePoints());
	}

	public Universe(Array<Vector2> points) {
		for (Vector2 p : points) {
			objects.add(new AstroBody(p));
		}
		this.points = points;
		//saveToJson();

	}


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

	@Override
	public void threadFinished(NoiseThread noiseThread) {
		NoiseBuffer noise = noiseThread.getNoise();
		loadedNoise.put(noise.seed, noise);
	}

	public NoiseBuffer getNoiseForSeed(long seed) {
		if (loadedNoise.containsKey(seed)) {
			return loadedNoise.get(seed);
		}
		return null;
	}
}
//endregion


public class SpaceLoadingSystem extends EntitySystem implements NoiseGenListener, EntityListener, Disposable {

	private Universe universe;
	private ImmutableArray<Entity> loadedAstronomicalBodies;
	private LinkedBlockingQueue<NoiseBuffer> noiseBufferQueue;
	NoiseThreadPoolExecutor noiseThreadPool;

	SimpleTimer checkStarsTimer;//this system might make sense as an interval system instead of timer

	@Override
	public void addedToEngine(Engine engine) {

		// currently loaded stars/planets
		loadedAstronomicalBodies = engine.getEntitiesFor(Family.all(BarycenterComponent.class, TransformComponent.class).get());

		engine.addEntityListener(Family.one(PlanetComponent.class, BarycenterComponent.class).get(),this);

		// generate universe
		universe = new Universe();

		// load space things (asteroids, wormhole, black hole, etc)
		// load ai/mobs

		noiseBufferQueue = new LinkedBlockingQueue<NoiseBuffer>();
		noiseThreadPool = new NoiseThreadPoolExecutor(SpaceProject.celestcfg.maxGenThreads);
		noiseThreadPool.addListener(this);
		noiseThreadPool.addListener(universe);

		checkStarsTimer = new SimpleTimer(4000);
	}


	@Override
	public void entityAdded(Entity entity) {
		PlanetComponent planet = Mappers.planet.get(entity);
		if (planet != null) {
			SeedComponent seedComp = Mappers.seed.get(entity);
			NoiseBuffer noiseBuffer = universe.getNoiseForSeed(seedComp.seed);

			//check noise map exists in universe file first, if so load into tileMap queue
			if (noiseBuffer!= null) {
				System.out.println("noise found, loading: " + seedComp.seed);
				noiseBufferQueue.add(noiseBuffer);
			} else {
				//TODO: prevent multiple threads on same workload(seed)
				// if (noiseThreadPool.getQueue().contains(seed))
				System.out.println("no noise found, generating: " + seedComp.seed);
				noiseThreadPool.execute(new NoiseThread(seedComp, planet, Tile.defaultTiles));
			}
		}
	}

	@Override
	public void entityRemoved(Entity entity) {
		//TODO: if has texture, dispose


		for (Entity e : getEngine().getEntitiesFor(Family.all(OrbitComponent.class).get())){
			OrbitComponent orbit = Mappers.orbit.get(e);
			if (orbit.parent != null) {
				if (orbit.parent == entity) {
					getEngine().removeEntity(e);
				}
			}
		}
	}

	@Override
	public void threadFinished(NoiseThread noise) {
		//TODO: save in universe file for caching and loading instantly when transition to world
		noiseBufferQueue.add(noise.getNoise());
	}

	
	@Override
	public void update(float delta) {
		// load and unload stars
		updateStars();

		// update/replace textures
		updatePlanetTextures();
	}


	private void updatePlanetTextures() {
		//check queue for tilemaps / pixmaps to load into textures
		if (!noiseBufferQueue.isEmpty()) {
			try {
				NoiseBuffer noise = noiseBufferQueue.take();
				if (noise.pixelatedTileMap == null) {
					System.out.println("ERROR, no map for: [" + noise.seed + "]");
					return;
				}

				for (Entity p : getEngine().getEntitiesFor(Family.all(PlanetComponent.class).get())) {
					if (p.getComponent(SeedComponent.class).seed == noise.seed) {//base on seed instead of ID, but what if duplicate seed? shouldn't happen..
					//if (p.getComponent(PlanetComponent.class).tempGenID == noise.ID) {
						// create planet texture from tileMap, replace texture
						Texture newTex = TextureFactory.generatePlanet(noise.pixelatedTileMap, Tile.defaultTiles);
						p.getComponent(TextureComponent.class).texture = newTex;
						System.out.println("Texture loaded: [" + noise.seed + "]");
						return;
					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	private void updateStars() {
		if (checkStarsTimer.canDoEvent()){
			checkStarsTimer.reset();
			
			//distance to check when to load planets
			int loadDistance = (int) SpaceProject.celestcfg.loadSystemDistance;
			loadDistance *= loadDistance;//square for dst2
			
			// remove stars from engine that are too far
			unloadFarEntities(loadDistance);

			// add planetary systems to engine
			loadCloseEntities(loadDistance);
		}
	}

	private void loadCloseEntities(int loadDistance) {
		for (AstroBody astroBodies : universe.objects) {
            //check if point is close enough to be loaded
            if (Vector2.dst2(astroBodies.x, astroBodies.y, GameScreen.cam.position.x, GameScreen.cam.position.y) < loadDistance) {

                // check if astro bodies already in world
                boolean loaded = false;
                for (Entity astroEntity : loadedAstronomicalBodies) {
                    SeedComponent s = Mappers.seed.get(astroEntity);
                    if (s.seed == astroBodies.seed) {
						loaded = true;
                        break;
                    }
                }

                if (!loaded) {
                    for (Entity e : EntityFactory.createAstronomicalObjects(astroBodies.x, astroBodies.y)) {
                        getEngine().addEntity(e);
                    }
                }

            }
        }
	}

	private void unloadFarEntities(int loadDistance) {
		for (Entity entity : loadedAstronomicalBodies) {
            TransformComponent t = Mappers.transform.get(entity);
            if (Vector2.dst2(t.pos.x, t.pos.y, GameScreen.cam.position.x, GameScreen.cam.position.y) > loadDistance) {
                getEngine().removeEntity(entity);
                System.out.println("Removing Planetary System: " + entity.getComponent(TransformComponent.class).pos.toString());
            }
        }
	}




	public Array<Vector2> getPoints() {
		return universe.points;
	}


	public void dispose() {
		noiseThreadPool.shutdown();
	}



}
