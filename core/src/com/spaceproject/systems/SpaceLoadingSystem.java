package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.Tile;
import com.spaceproject.components.BarycenterComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.AstroBody;
import com.spaceproject.generation.EntityFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.generation.noise.NoiseBuffer;
import com.spaceproject.generation.noise.NoiseThread;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class SpaceLoadingSystem extends EntitySystem implements EntityListener {

	//private Universe universe;
	private ImmutableArray<Entity> loadedAstronomicalBodies;
	//private LinkedBlockingQueue<NoiseBuffer> noiseBufferQueue;
	//NoiseThreadPoolExecutor noiseThreadPool;

	SimpleTimer checkStarsTimer;//this system might make sense as an interval system instead of timer

	/*
	public SpaceLoadingSystem(NoiseThreadPoolExecutor noiseThreadPool) {
		noiseThreadPool.addListener(this);
	}*/

	@Override
	public void addedToEngine(Engine engine) {

		// currently loaded stars/planets
		loadedAstronomicalBodies = engine.getEntitiesFor(Family.all(BarycenterComponent.class, TransformComponent.class).get());

		engine.addEntityListener(Family.one(PlanetComponent.class, BarycenterComponent.class).get(),this);

		// generate universe
		//universe = new Universe();

		// load space things (asteroids, wormhole, black hole, etc)
		// load ai/mobs

		//noiseBufferQueue = new LinkedBlockingQueue<NoiseBuffer>();
		//noiseThreadPool = new NoiseThreadPoolExecutor(SpaceProject.celestcfg.maxGenThreads);
		//noiseThreadPool.addListener(this);
		//noiseThreadPool.addListener(universe);

		checkStarsTimer = new SimpleTimer(4000);
	}


	@Override
	public void entityAdded(Entity entity) {
		PlanetComponent planet = Mappers.planet.get(entity);
		if (planet != null) {
			SeedComponent seedComp = Mappers.seed.get(entity);
			NoiseBuffer noiseBuffer = GameScreen.universe.getNoiseForSeed(seedComp.seed);

			//check noise map exists in universe file first, if so load into tileMap queue
			if (noiseBuffer != null) {
				Gdx.app.log(this.getClass().getSimpleName(), "noise found, loading: " + seedComp.seed);
				GameScreen.noiseBufferQueue.add(noiseBuffer);
			} else {

				//TODO: prevent multiple threads on same workload(seed)
				// if (noiseThreadPool.getQueue().contains(seed))
				Gdx.app.log(this.getClass().getSimpleName(), "no noise found, generating: " + seedComp.seed);
				GameScreen.noiseThreadPool.execute(new NoiseThread(seedComp, planet, Tile.defaultTiles));
			}
		}
	}

	@Override
	public void entityRemoved(Entity entity) {
		for (Entity e : getEngine().getEntitiesFor(Family.all(OrbitComponent.class).get())) {
			OrbitComponent orbit = Mappers.orbit.get(e);
			if (orbit.parent != null) {
				if (orbit.parent == entity) {
					getEngine().removeEntity(e);
				}
			}
		}
	}


	/*
	@Override
	public void threadFinished(NoiseThread noise) {
		//TODO: save in universe file for caching and loading instantly when transition to world
		GameScreen.noiseBufferQueue.add(noise.getNoise());
	}
	*/
	
	@Override
	public void update(float delta) {
		// load and unload stars
		updateStars();

		// update/replace textures
		updatePlanetTextures();
	}


	private void updatePlanetTextures() {
		//check queue for tilemaps / pixmaps to load into textures
		if (!GameScreen.noiseBufferQueue.isEmpty()) {
			try {
				NoiseBuffer noise = GameScreen.noiseBufferQueue.take();
				if (noise.pixelatedTileMap == null) {
					Gdx.app.log(this.getClass().getSimpleName(), "ERROR, no map for: [" + noise.seed + "]");
					return;
				}

				for (Entity p : getEngine().getEntitiesFor(Family.all(PlanetComponent.class).get())) {
					if (p.getComponent(SeedComponent.class).seed == noise.seed) {//base on seed instead of ID, but what if duplicate seed? shouldn't happen..
					//if (p.getComponent(PlanetComponent.class).tempGenID == noise.ID) {
						// create planet texture from tileMap, replace texture
						Texture newTex = TextureFactory.generatePlanet(noise.pixelatedTileMap, Tile.defaultTiles);
						p.getComponent(TextureComponent.class).texture = newTex;
						Gdx.app.log(this.getClass().getSimpleName(), "Texture loaded: [" + noise.seed + "]");
						return;
					}
				}

			} catch (InterruptedException e) {
				Gdx.app.error(this.getClass().getSimpleName(), "error updating planet texture", e);
			}
		}
	}


	private void updateStars() {
		if (checkStarsTimer.tryEvent()) {
			
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
		for (AstroBody astroBodies : GameScreen.universe.objects) {
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
				Gdx.app.log(this.getClass().getSimpleName(), "Removing Planetary System: " + entity.getComponent(TransformComponent.class).pos.toString());
            }
        }
	}

	public Array<Vector2> getPoints() {
		return GameScreen.universe.points;
	}


}
