package com.spaceproject.systems;

import java.util.ArrayList;
import java.util.Comparator;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.spaceproject.SpaceProject;
import com.spaceproject.Tile;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.NoiseGen;

public class WorldRenderingSystem extends IteratingSystem implements Disposable {
		
	private Array<Entity> renderQueue; //array of entities to render
	private Comparator<Entity> comparator; //for sorting render order
	
	//rendering
	private static OrthographicCamera cam;
	private ExtendViewport viewport;
	private SpriteBatch batch;
	
	//window size
	private int prevWindowWidth = 0;
	private int prevWindowHeight = 0;
	
	//vertical sync
	private boolean vsync = true;
	
	//camera zoom
	private float zoomTarget = 1;
	
	private static final int WORLDHEIGHT = 720;
	
	private ArrayList<Tile> tiles = Tile.defaultTiles;
	
	private float[][] heightMap;//height of tile
	private int[][] tileMap;//index of tile
		
	private int tileSize; //render size of tiles
	private int surround; //how many tiles to draw around the camera

	static Texture tileTex = TextureFactory.createTile(new Color(1f, 1f, 1f, 1f));
	
	public WorldRenderingSystem(PlanetComponent planet, OrthographicCamera camera) {
		super(Family.all(TransformComponent.class, TextureComponent.class).get());
		
		cam = camera;
		
		renderQueue = new Array<Entity>();
		
		//sort by depth, z axis determines what order to draw 
		comparator = new Comparator<Entity>() {
			@Override
			public int compare(Entity entityA, Entity entityB) {
				return (int)Math.signum(Mappers.transform.get(entityB).pos.z -
										Mappers.transform.get(entityA).pos.z);
			}
		};
		
		//initialize camera, viewport and aspect ratio
		float aspectRatio = Gdx.graphics.getHeight() / Gdx.graphics.getWidth();	
		viewport = new ExtendViewport(WORLDHEIGHT * aspectRatio, WORLDHEIGHT, camera);
		viewport.apply();
		
		batch = new SpriteBatch();
	
		
		tileSize = 32;
		surround = 30;	
				
		loadMap(planet);
		
		//set vsync off for development, on by default
		toggleVsync();
	
	}

	private void loadMap(PlanetComponent planet) {
		//TODO: use NoiseThread
		
		//create tile features
		//createTiles();

		//create world features
		/*
		float scale = 100; //scale of noise = 40;
		int octaves = 4;
		float persistence = 0.68f;//0 - 1
		float lacunarity = 2.6f;//1 - x
		int mapSize = 256; //size of world	*/	
		heightMap = NoiseGen.generateWrappingNoise4D(planet.seed, planet.mapSize, planet.scale, planet.octaves, planet.persistence, planet.lacunarity);
		
		//create map of tiles based on height
		tileMap = NoiseGen.createTileMap(heightMap, tiles);		
	}
	
	
	/*
	private void createTiles() {
		tiles.add(new Tile("water",  0.41f,  Color.BLUE));
		tiles.add(new Tile("water1", 0.345f, new Color(0,0,0.42f,1)));
		tiles.add(new Tile("water2", 0.240f, new Color(0,0,0.23f,1)));
		tiles.add(new Tile("water3", 0.085f, new Color(0,0,0.1f,1)));
		tiles.add(new Tile("sand",   0.465f, Color.YELLOW));
		tiles.add(new Tile("grass",  0.625f, Color.GREEN));
		tiles.add(new Tile("grass1", 0.725f, new Color(0,0.63f,0,1)));
		tiles.add(new Tile("grass2", 0.815f, new Color(0,0.48f,0,1)));
		tiles.add(new Tile("lava",   1f,     Color.RED));
		tiles.add(new Tile("rock",   0.95f,  Color.BROWN));
		Collections.sort(tiles);
	}*/

	@Override
	public void update(float delta) {
		super.update(delta); //adds entities to render queue
			
		//clear screen
		Gdx.gl20.glClearColor(0, 0, 0, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//sort render order of entities
		renderQueue.sort(comparator); 
		
		//update camera and projection
		cam.update();
		batch.setProjectionMatrix(cam.combined); 
		
		//draw
		batch.begin();
		
		//render background tiles
		drawTiles();
		
		batch.setColor(Color.WHITE);
		
		//render all textures
		for (Entity entity : renderQueue) {
			TextureComponent tex = Mappers.texture.get(entity);
		
			if (tex.texture == null) continue;
			
			TransformComponent t = Mappers.transform.get(entity);
		
			float width = tex.texture.getWidth();
			float height = tex.texture.getHeight();
			float originX = width * 0.5f; //center 
			float originY = height * 0.5f; //center

			//draw texture
			batch.draw(tex.texture, (t.pos.x - originX), (t.pos.y - originY),
					   originX, originY,
					   width, height,
					   tex.scale, tex.scale,
					   MathUtils.radiansToDegrees * t.rotation, 
					   0, 0, (int)width, (int)height, false, false);
		}
		batch.end();
		
		renderQueue.clear();
		
		
		//adjust zoom
		zoomCamera(delta);
		
		//TODO: move into input
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.rotateRight)) {
			cam.rotate(5f * delta);
		}
		if (Gdx.input.isKeyPressed(SpaceProject.keycfg.rotateLeft)) {
			cam.rotate(-5f * delta);
		}
	}	

	private void drawTiles() {
			
		// calculate tile that the camera is in
		int centerX = (int) (cam.position.x / tileSize);
		int centerY = (int) (cam.position.y / tileSize);

		// subtract 1 from tile position if less than zero to account for -1/n = 0
		if (cam.position.x < 0) --centerX;		
		if (cam.position.y < 0) --centerY;
		
		for (int tileY = centerY - surround; tileY <= centerY + surround; tileY++) {
			for (int tileX = centerX - surround; tileX <= centerX + surround; tileX++) {
				
				//wrap tiles when position is outside of map
				int tX = tileX % heightMap.length;
				int tY = tileY % heightMap.length;
				if (tX < 0) tX += heightMap.length;
				if (tY < 0) tY += heightMap.length;
							
				//render tile
				batch.setColor(tiles.get(tileMap[tX][tY]).getColor());
				//if (tX == heightMap.length-1 || tY == heightMap.length-1) batch.setColor(Color.BLACK);
				batch.draw(tileTex, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
			}
		}
		
		
		/*//debug draw full map
		for (int x = 0; x < mapSize; ++x) {
			for (int y = 0; y < mapSize; ++y) {
				if (map[x][y] > 0.90) {
					batch.draw(rockDark, x * tileSize, y * tileSize, tileSize, tileSize);
				} else if (map[x][y] > 0.80) {
					batch.draw(rockLight, x * tileSize, y * tileSize, tileSize, tileSize);
				} else if (map[x][y] > 0.70) {
					batch.draw(grassDark, x * tileSize, y * tileSize, tileSize, tileSize);
				} else if (map[x][y] > 0.53) {
					batch.draw(grassLight, x * tileSize, y * tileSize, tileSize, tileSize);
				} else if (map[x][y] > 0.50) {
					batch.draw(sand, x * tileSize, y * tileSize, tileSize, tileSize);
				} else if (map[x][y] > 0.40) {
					batch.draw(waterShallow, x * tileSize, y * tileSize, tileSize, tileSize);
				} else if (map[x][y] > 0.25) {
					batch.draw(waterDeep, x * tileSize, y * tileSize, tileSize, tileSize);
				} else {
					batch.draw(waterDeeper, x * tileSize, y * tileSize, tileSize, tileSize);
				}			
			}
		}*/
		/*
		for (int tileY = centerY - surround; tileY <= centerY + surround; tileY++) {
			for (int tileX = centerX - surround; tileX <= centerX + surround; tileX++) {			
				//wrap tiles when position is outside of map
				int tX = tileX % heightMap.length;
				int tY = tileY % heightMap.length;
				if (tX < 0) tX += heightMap.length;
				if (tY < 0) tY += heightMap.length;
				
				//draw tiles
				//TODO: refactor threshold values to config object
				if (heightMap[tX][tY] > 0.90) {
					batch.draw(rockDark, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
				} else if (heightMap[tX][tY] > 0.80) {
					batch.draw(rockLight, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
				} else if (heightMap[tX][tY] > 0.70) {
					batch.draw(grassDark, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
				} else if (heightMap[tX][tY] > 0.53) {
					batch.draw(grassLight, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
				} else if (heightMap[tX][tY] > 0.50) {
					batch.draw(sand, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
				} else if (heightMap[tX][tY] > 0.40) {
					batch.draw(waterShallow, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
				} else if (heightMap[tX][tY] > 0.25) {
					batch.draw(waterDeep, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
				} else {
					batch.draw(waterDeeper, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
				}
			}
		}*/
	}


	
	/**
	 * Animate camera zoom. Will zoom camera in or out until it reaches zoomTarget.
	 * @param delta
	 */
	private void zoomCamera(float delta) {
		if (cam.zoom != zoomTarget) {
			float scaleSpeed = 3 * delta;
			//zoom in/out
			cam.zoom += (cam.zoom < zoomTarget) ? scaleSpeed : -scaleSpeed;

			//if zoom is close enough, just set it to target
			if (Math.abs(cam.zoom - zoomTarget) < 0.1) {
				cam.zoom = zoomTarget;
			}
		}
	}

	/**
	 * Turn vsync on or off
	 */
	void toggleVsync() {
		vsync = !vsync;
		Gdx.graphics.setVSync(vsync);
		System.out.println("vsync: " + vsync);
	}

	/**
	 * Switch between fullscreen and windowed mode.
	 */
	void toggleFullscreen() {
		if (Gdx.graphics.isFullscreen()) {
			//set window to previous window size
			Gdx.graphics.setWindowedMode(prevWindowWidth, prevWindowHeight);
		} else {
			//save window size
			prevWindowWidth = Gdx.graphics.getWidth();
			prevWindowHeight = Gdx.graphics.getHeight();
			
			//set to fullscreen
			if (Gdx.graphics.supportsDisplayModeChange()) {
				Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
			} else {
				Gdx.app.log("graphics", "DisplayModeChange not supported.");
			}
		}
	}
	
	@Override
	public void processEntity(Entity entity, float deltaTime) {
		//Add entities to render queue
		renderQueue.add(entity);
	}
	
	/**
	 * Set zoom for camera to animate to.
	 * @param zoom
	 */
	public void setZoomTarget(float zoom) {
		zoomTarget = zoom;
	}
	
	public static float getCamZoom() {
		return cam.zoom;
	}

	public static Vector3 getCamPos() {
		return cam.position;
	}
	
	public static OrthographicCamera getCam() {
		return cam;
	}

	/**
	 * Resize viewport. Called from screen resize.
	 * @param width
	 * @param height
	 */
	public void resize(int width, int height) {
		viewport.update(width, height);
	}


	@Override
	public void dispose() {
		
		//dispose of all textures
		for (Entity entity : renderQueue) {
			TextureComponent tex = Mappers.texture.get(entity);	
			if (tex.texture != null)
				tex.texture.dispose();
		}
		
		//batch.dispose();//crashes: 
		/*
		EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x0000000054554370, pid=5604, tid=2364
		Problematic frame:
	 	C  [atio6axx.dll+0x3c4370]
		 */
	}


	

}
