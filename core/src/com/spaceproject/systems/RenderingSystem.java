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
	
	//TODO change all entity savings to a player/focus component
	public Entity playerEntity = null; //the player entity target reference
	
	
	//tile stuff
	//public SpaceBackgroundTile[][] spaceBackgroundB = new SpaceBackgroundTile[3][3];
	
	//public Array<SpaceBackgroundTile> spaceBackground = new Array<SpaceBackgroundTile>();
	public ArrayList<SpaceBackgroundTile> spaceBackground = new ArrayList<SpaceBackgroundTile>();
	
	static int tileSize = 1024; //how large a world space is
	private float checkTileTimer = 500;
	private float checkTileCurrTime = checkTileTimer;
	private Vector2 currentCenterTile;
	int surround = 2;//how many tiles to load around center of player
	
	
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
		//TODO: account for tile depth
		
		int x = (int)posX / tileSize;
		int y = (int)posY / tileSize;
		
		if (posX < 0) {
			x--;
		}
		if (posY < 0) {
			y--;
		}
		
		//System.out.println("tile: " + x + " ," + y);
		
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
		for (int tX = (int)currentCenterTile.x - surround; tX <= (int)currentCenterTile.x + surround; tX++) {
			for (int tY = (int)currentCenterTile.y - surround; tY <= (int)currentCenterTile.y + surround; tY++) {				
				spaceBackground.add(new SpaceBackgroundTile(tX, tY, 0.5f));
			}
		}
	
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
		
		//render background stars
		for (SpaceBackgroundTile back : spaceBackground) {
			batch.draw(back.tex, back.x + (cam.position.x - (tileSize/2)) * back.depth, back.y + (cam.position.y - (tileSize/2)) * back.depth);
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
		

		//tile loading------------------------------------------
		checkTileCurrTime -= 1000 * delta;
		if (checkTileCurrTime < 0) {
			
			//get tile player is in
			TransformComponent pos = transformMap.get(playerEntity);			
			Vector2 newTile = getTilePos(pos.pos.x, pos.pos.y);
			
			//check if player has changed tiles
			if (newTile.x != currentCenterTile.x || newTile.y != currentCenterTile.y) { 
				
				
				//unload old tiles				
				for (int index = 0; index < spaceBackground.size(); ++index) {
					//remove any tiles not surrounding player
					if (spaceBackground.get(index).tileX < newTile.x - surround || spaceBackground.get(index).tileX > newTile.x + surround 
							|| spaceBackground.get(index).tileY < newTile.y - surround || spaceBackground.get(index).tileY > newTile.y + surround) {
						
						spaceBackground.remove(index);
						
						index = -1;
						if (index >= spaceBackground.size()) {
							continue;
						}
					}
				}

				//load new tiles
				for (int tX = (int)newTile.x - surround; tX <= (int)newTile.x + surround; tX++) {
					for (int tY = (int)newTile.y - surround; tY <= (int)newTile.y + surround; tY++) {
						boolean exists = false; //is the tile already loaded
						for (int index = 0; index < spaceBackground.size() && !exists; ++index) {
							if (spaceBackground.get(index).tileX == tX && spaceBackground.get(index).tileY == tY) {					
								exists = true;
								//break;
							}
						}
						if (!exists) {
							spaceBackground.add(new SpaceBackgroundTile(tX, tY, 0.5f));
						}
						
					}
				}
				//spaceBackground.add(new SpaceBackgroundTile(tX, tY, 0.5f));
				/*
				//left tiles = center tiles
				spaceBackgroundB[0][0] = spaceBackgroundB[1][0]; //left,bottom = center,bottom
				spaceBackgroundB[0][1] = spaceBackgroundB[1][1]; //left,center = center,center
				spaceBackgroundB[0][2] = spaceBackgroundB[1][2]; //left,top	   = center,top				
				
				//center tiles = right tiles
				spaceBackgroundB[1][0] = spaceBackgroundB[2][0]; //center,bottom = right,bottom
				spaceBackgroundB[1][1] = spaceBackgroundB[2][1]; //center,center = right,center
				spaceBackgroundB[1][2] = spaceBackgroundB[2][2]; //center,top	 = right,top
				
				//load new right tiles
				spaceBackgroundB[2][0] = new SpaceBackgroundTile((int)newTile.x + 1, (int)newTile.y - 1, 0.5f); // right, bottom
				spaceBackgroundB[2][1] = new SpaceBackgroundTile((int)newTile.x + 1, (int)newTile.y, 	   0.5f); // right, center
				spaceBackgroundB[2][2] = new SpaceBackgroundTile((int)newTile.x + 1, (int)newTile.y + 1, 0.5f); // right, top
				*/
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
