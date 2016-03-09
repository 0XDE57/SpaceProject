package com.spaceproject.systems;

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
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.OpenSimplexNoise;

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
	
	//TODO: come up with some kind of standard size (pixel to meters)? / something less arbitrary
	//private static final int WORLDWIDTH = 1280;
	private static final int WORLDHEIGHT = 720;
	
	private Texture water;
	private Texture grass;
	
	private double map[][];	
	
	private float scale;
	int mapSize = 128;
	int tileSize = 40;

	
	public WorldRenderingSystem(OrthographicCamera camera) {
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
		
		
		water = TextureFactory.createTile(Color.BLUE);
		grass = TextureFactory.createTile(Color.GREEN);
		
		scale = 40;
		
		map = new double[mapSize][mapSize];
		
		//generate map
		OpenSimplexNoise noise = new OpenSimplexNoise();	
		for (int y = 0; y < mapSize; ++y) {
			for (int x = 0; x < mapSize; ++x) {
				
				//sinX, cosX. wrap X axis
				double sx = MathUtils.sin(x * MathUtils.PI2 / mapSize) / MathUtils.PI2 * mapSize / scale;
				double cx = MathUtils.cos(x * MathUtils.PI2 / mapSize) / MathUtils.PI2 * mapSize / scale;
				//sinY, cosY. wrap Y axis
				double sy = MathUtils.sin(y * MathUtils.PI2 / mapSize) / MathUtils.PI2 * mapSize / scale;
				double cy = MathUtils.cos(y * MathUtils.PI2 / mapSize) / MathUtils.PI2 * mapSize / scale;
				
				
				double i = noise.eval(sx, cx, sy, cy); //get 4D noise using wrapped x and y axis
				//i = (i * 0.5) + 0.5; // convert from range [-1:1] to [0:1]
				map[x][y] = i;
			}
		}
		
		//set vsync off for development, on by default
		toggleVsync();
	
	}

	
	@Override
	public void update(float delta) {
		super.update(delta); //adds entities to render queue
			
		//clear screen with color based on camera position
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
		for (int x = 0; x < mapSize; ++x) {
			for (int y = 0; y < mapSize; ++y) {
				if (map[x][y] > 0) {
					batch.draw(water, x * tileSize, y * tileSize, tileSize, tileSize);
				} else {
					batch.draw(grass, x * tileSize, y * tileSize, tileSize, tileSize);
				}
			}
		}
			
		
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
