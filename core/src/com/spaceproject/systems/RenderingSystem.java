package com.spaceproject.systems;

import java.util.ArrayList;
import java.util.Comparator;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.spaceproject.SpaceBackgroundTile;
import com.spaceproject.SpaceProject;
import com.spaceproject.ThreadLoadTest;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;

public class RenderingSystem extends IteratingSystem {
		
	private Array<Entity> renderQueue; //array of entities to render
	private Comparator<Entity> comparator; //for sorting render order
	
	//map for image and position
	private ComponentMapper<TextureComponent> textureMap;
	private ComponentMapper<TransformComponent> transformMap;
	
	private static OrthographicCamera cam;
	private ExtendViewport viewport;
	private static SpriteBatch batch;
	
	//window size
	private int prevWindowWidth = 0;
	private int prevWindowHeight = 0;
	
	//vertical sync
	private boolean vsync = true;
	
	//camera zoom
	private float zoomTarget = 1;
	
	//TODO: come up with some kind of standard size (pixel to meters)? / something less arbitrary
	private static final int WORLDWIDTH = 1280;
	private static final int WORLDHEIGHT = 720;
	

	//private Array<SpaceBackgroundTile>[][] backgroundStarsLayer = (Array<SpaceBackgroundTile>[][])new Array[3][3];	
	//private Array<SpaceBackgroundTile> backgroundStarsLayer1 = new Array<SpaceBackgroundTile>();
	
	public SpaceBackgroundTile[][] spaceBackground = new SpaceBackgroundTile[3][3];
	
	//TODO change all entity savings to a player/focus component
	public Entity playerEntity = null; //the player entity target reference
	
	//Texture starTexture; //holds single pixel for star
	static int tileSize = 3200; //how large a world space is
	private float checkTileTimer = 2000;
	private float checkTileCurrTime = checkTileTimer;
	private Vector2 currentCenterTile;
	
	
	
	public void loadTile(int tileX, int tileY) {	
		System.out.println("loading tile: " + tileX + ", " + tileY);			

		//load stars into tile		
		//load spacedust/background clouds(noise/fractals)
		//load planetary systems / planets / stars
		//load space things (asteroids, wormhole/ black hole/ etc
		//load ai/mobs
		
	}
	
	/** Convert world position to tile position.  
	 * @param posX
	 * @param posY
	 * @return tile that an object is in.
	 */
	public static Vector2 getTilePos(float posX, float posY) {
		int x = (int)posX / tileSize;
		int y = (int)posY / tileSize;
		
		if (posX < 0) {
			x--;
		}
		if (posY < 0) {
			y--;
		}
		
		return new Vector2(x, y);
	}
	
	@SuppressWarnings("unchecked")
	public RenderingSystem(Entity player) {
		super(Family.all(TransformComponent.class, TextureComponent.class).get());
	
		textureMap = ComponentMapper.getFor(TextureComponent.class);
		transformMap = ComponentMapper.getFor(TransformComponent.class);
		
		renderQueue = new Array<Entity>();
		
		//sort by depth, z axis determines what order to draw 
		comparator = new Comparator<Entity>() {
			@Override
			public int compare(Entity entityA, Entity entityB) {
				return (int)Math.signum(transformMap.get(entityB).pos.z -
										transformMap.get(entityA).pos.z);
			}
		};
		
		//initialize camera, viewport and aspect ratio
		cam = new OrthographicCamera();
		float aspectRatio = Gdx.graphics.getHeight() / Gdx.graphics.getWidth();	
		viewport = new ExtendViewport(WORLDHEIGHT * aspectRatio, WORLDHEIGHT, cam);
		viewport.apply();
		
		batch = new SpriteBatch();
		
		//set vsync off for development, on by default
		toggleVsync();		
		
		//target for tiles
		this.playerEntity = player;
		TransformComponent pos = transformMap.get(playerEntity);
		currentCenterTile = getTilePos(pos.pos.x, pos.pos.y);
		
		//////////load tiles//////////
		/*
		spaceBackground[0][0] = new SpaceBackgroundTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y - 1, 0.5f); // left, bottom
		spaceBackground[0][1] = new SpaceBackgroundTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y, 	   0.5f); // left, center
		spaceBackground[0][2] = new SpaceBackgroundTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y + 1, 0.5f); // left, top
								
		spaceBackground[1][0] = new SpaceBackgroundTile((int)currentCenterTile.x,	 (int)currentCenterTile.y - 1, 0.5f); // center, bottom
		spaceBackground[1][1] = new SpaceBackgroundTile((int)currentCenterTile.x, 	 (int)currentCenterTile.y,     0.5f); // center, center
		spaceBackground[1][2] = new SpaceBackgroundTile((int)currentCenterTile.x, 	 (int)currentCenterTile.y + 1, 0.5f); // center, top
		
		spaceBackground[2][0] = new SpaceBackgroundTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y - 1, 0.5f); // right, bottom
		spaceBackground[2][1] = new SpaceBackgroundTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y, 	   0.5f); // right, center
		spaceBackground[2][2] = new SpaceBackgroundTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y + 1, 0.5f); // right, top
		*/
		
		
		Thread testThead = new ThreadLoadTest(this);
		testThead.start();
		
		/*
		bg[1][1] = loadTile((int)currentCenterTile.x, (int)currentCenterTile.y); 	   // center, center
		bg[1][2] = loadTile((int)currentCenterTile.x, (int)currentCenterTile.y + 1); // center, top
		bg[1][0] = loadTile((int)currentCenterTile.x,	(int)currentCenterTile.y - 1); // center, bottom
		
		bg[2][0] = loadTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y - 1); // right, bottom
		bg[2][1] = loadTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y); 	   // right, center
		bg[2][2] = loadTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y + 1); // right, top
							
		bg[0][1] = loadTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y); 	   // left, center
		bg[0][2] = loadTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y + 1); // left, top
		bg[0][0] = loadTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y - 1); // left, bottom
		
		
		loadTile((int)currentCenterTile.x, (int)currentCenterTile.y); 	   // center, center
		loadTile((int)currentCenterTile.x, (int)currentCenterTile.y + 1); // center, top
		loadTile((int)currentCenterTile.x,	(int)currentCenterTile.y - 1); // center, bottom
		
		loadTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y - 1); // right, bottom
		loadTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y); 	   // right, center
		loadTile((int)currentCenterTile.x + 1, (int)currentCenterTile.y + 1); // right, top
							
		loadTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y); 	   // left, center
		loadTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y + 1); // left, top
		loadTile((int)currentCenterTile.x - 1, (int)currentCenterTile.y - 1); // left, bottom*/
	}
	
	
	@Override
	public void update(float delta) {
		super.update(delta); //adds entities to render queue
		
		//clear screen with black
		Gdx.gl20.glClearColor(0, 0, 0, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		
		renderQueue.sort(comparator); //sort render order of entities
		
		//update camera and projection
		cam.update();
		batch.setProjectionMatrix(cam.combined); 
		
		batch.begin();


		//render background
		for (int i = 0; i < spaceBackground.length; ++i) {
			for (int j = 0; j < spaceBackground.length; ++j) {
				SpaceBackgroundTile back = spaceBackground[i][j];
				//batch.draw(back.tex, back.x, back.y);
				batch.draw(back.tex, back.x + (cam.position.x - (tileSize/2)) * back.depth, back.y + (cam.position.y - (tileSize/2)) * back.depth);
				
			}
		}

		//render all textures
		for (Entity entity : renderQueue) {
			TextureComponent tex = textureMap.get(entity);
		
			if (tex.texture == null) continue;
			
			TransformComponent t = transformMap.get(entity);
		
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
		

		//tile loading
		checkTileCurrTime -= 1000 * delta;
		if (checkTileCurrTime < 0) {
			TransformComponent pos = transformMap.get(playerEntity);
			Vector2 newTile = getTilePos(pos.pos.x, pos.pos.y);
			if (newTile.x > currentCenterTile.x) {
				System.out.println("--------Moved tiles RIGHT---------");
				//left tiles = center tiles
				spaceBackground[0][0] = spaceBackground[1][0]; //left,bottom = center,bottom
				spaceBackground[0][1] = spaceBackground[1][1]; //left,center = center,center
				spaceBackground[0][2] = spaceBackground[1][2]; //left,top	   = center,top				
				
				//center tiles = right tiles
				spaceBackground[1][0] = spaceBackground[2][0]; //center,bottom = right,bottom
				spaceBackground[1][1] = spaceBackground[2][1]; //center,center = right,center
				spaceBackground[1][2] = spaceBackground[2][2]; //center,top	 = right,top
				
				//load new right tiles
				spaceBackground[2][0] = new SpaceBackgroundTile((int)newTile.x + 1, (int)newTile.y - 1, 0.5f); // right, bottom
				spaceBackground[2][1] = new SpaceBackgroundTile((int)newTile.x + 1, (int)newTile.y, 	   0.5f); // right, center
				spaceBackground[2][2] = new SpaceBackgroundTile((int)newTile.x + 1, (int)newTile.y + 1, 0.5f); // right, top
				
				/*
				//left tiles = center tiles
				backgroundStarsLayer[0][0] = backgroundStarsLayer[1][0]; //left,bottom = center,bottom
				backgroundStarsLayer[0][1] = backgroundStarsLayer[1][1]; //left,center = center,center
				backgroundStarsLayer[0][2] = backgroundStarsLayer[1][2]; //left,top	   = center,top				
				
				//center tiles = right tiles
				backgroundStarsLayer[1][0] = backgroundStarsLayer[2][0]; //center,bottom = right,bottom
				backgroundStarsLayer[1][1] = backgroundStarsLayer[2][1]; //center,center = right,center
				backgroundStarsLayer[1][2] = backgroundStarsLayer[2][2]; //center,top	 = right,top
				
				//load new right tiles
				backgroundStarsLayer[2][0] = loadTile((int)newTile.x + 1, (int)newTile.y - 1); // right, bottom
				backgroundStarsLayer[2][1] = loadTile((int)newTile.x + 1, (int)newTile.y); // right, center
				backgroundStarsLayer[2][2] = loadTile((int)newTile.x + 1, (int)newTile.y + 1); // right, top
				*/
			}
			
			currentCenterTile = newTile;			
			checkTileCurrTime = checkTileTimer;
		}
		
	
		//adjust zoom
		if (cam.zoom != zoomTarget) {
			float scaleSpeed = 3 * delta;
			//zoom in/out
			cam.zoom += (cam.zoom < zoomTarget) ? scaleSpeed : -scaleSpeed;

			//if zoom is close enough, just set it to target
			if (Math.abs(cam.zoom - zoomTarget) < 0.1) {
				cam.zoom = zoomTarget;
			}
		}
		
		if (Gdx.input.isKeyPressed(Keys.LEFT_BRACKET)) {
			cam.rotate(5f * delta);
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT_BRACKET)) {
			cam.rotate(-5f * delta);
		}
	}

	//turn vsync on or off
	void toggleVsync() {
		vsync = !vsync;
		Gdx.graphics.setVSync(vsync);
		System.out.println("vsync: " + vsync);
	}

	//switch between fullscreen and windowed
	void toggleFullscreen() {
		if (Gdx.graphics.isFullscreen()) {
			//set window to previous window size
			Gdx.graphics.setDisplayMode(prevWindowWidth, prevWindowHeight, false);
		} else {
			//save windows size
			prevWindowWidth = Gdx.graphics.getWidth();
			prevWindowHeight = Gdx.graphics.getHeight();
			
			//set to fullscreen
			if (Gdx.graphics.supportsDisplayModeChange()) {
				Gdx.graphics.setDisplayMode(Gdx.graphics.getDesktopDisplayMode().width, Gdx.graphics.getDesktopDisplayMode().height, true); 
			} else {
				Gdx.app.log("graphics", "DisplayModeChange not supported.");
			}
		}
	}
	
	@Override
	//Add entities to render queue
	public void processEntity(Entity entity, float deltaTime) {
		renderQueue.add(entity);
	}
	
	//set zoom target
	public void zoom(float zoom) {
		zoomTarget = zoom;
		System.out.println("zoom: " + zoom);
	}

	
	public static OrthographicCamera getCam() {
		return cam;
	}

	//resize viewport. called from screen resize
	public void resize(int width, int height) {
		viewport.update(width, height);
	}

	

}
