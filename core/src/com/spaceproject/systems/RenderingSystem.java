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
	private ArrayList<SpaceBackgroundTile> spaceBackgroundLayer1 = new ArrayList<SpaceBackgroundTile>();
	private ArrayList<SpaceBackgroundTile> spaceBackgroundLayer2 = new ArrayList<SpaceBackgroundTile>();
	private static float backgroundDepth1 = 1f;
	private static float backgroundDepth2 = 0.8f;
	private static int tileSize = 1024; //how large a star tile is
	private float checkTileTimer = 750; //how often to check if player moved tiles
	private float checkTileCurrTime = checkTileTimer;
	private Vector2 currentCenterTile; //local tile player is on to check for movement
	private int surround = 1;//how many tiles to load around center of player
	
	
	public void loadTile(int tileX, int tileY) {	
		System.out.println("loading tile: " + tileX + ", " + tileY);			

		//world loading system?
		
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
		//tile.x + (cam.position.x - (tile.size/2)) * tile.depth, tile.y + (cam.position.y - (tile.size/2)) * tile.depth
		
		//int x = (int)posX / tileSize;
		//int y = (int)posY / tileSize;	
		
		
		int x = (int) ((posX * backgroundDepth1) / tileSize);
		int y = (int) ((posY * backgroundDepth1) / tileSize);
		
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
			
		//////////load tiles//////////	
		TransformComponent pos = transformMap.get(playerEntity);
		currentCenterTile = getTilePos(pos.pos.x, pos.pos.y);
		for (int tX = (int)currentCenterTile.x - surround; tX <= (int)currentCenterTile.x + surround; tX++) {
			for (int tY = (int)currentCenterTile.y - surround; tY <= (int)currentCenterTile.y + surround; tY++) {				
				spaceBackgroundLayer1.add(new SpaceBackgroundTile(tX, tY, backgroundDepth1, tileSize));
				//spaceBackgroundLayer1.add(new SpaceBackgroundTile(tX, tY, backgroundDepth2, tileSize));
			}
		}
	
	}

	
	@Override
	public void update(float delta) {
		super.update(delta); //adds entities to render queue
		
		//clear screen with black
		Gdx.gl20.glClearColor(0, 0, 0, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//sort render order of entities
		renderQueue.sort(comparator); 
		
		//update camera and projection
		cam.update();
		batch.setProjectionMatrix(cam.combined); 
		
		//draw
		batch.begin();	
		
		//render background stars
		for (SpaceBackgroundTile tile : spaceBackgroundLayer1) {
			batch.draw(tile.tex, tile.x + (cam.position.x - (tile.size/2)) * tile.depth, tile.y + (cam.position.y - (tile.size/2)) * tile.depth);
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
		//TODO: move into own system
		checkTileCurrTime -= 1000 * delta;
		if (checkTileCurrTime < 0) {
			
			//get tile player is in
			TransformComponent pos = transformMap.get(playerEntity);			
			Vector2 newTile = getTilePos(pos.pos.x, pos.pos.y);
			
			//check if player has changed tiles
			if (newTile.x != currentCenterTile.x || newTile.y != currentCenterTile.y) { 
				System.out.println("---< tile change: " + newTile.x + "," + newTile.y + " ---- " + pos.pos.x + "," + pos.pos.y + " >---");
				
				//unload old tiles-------------------------------------------------------		
				for (int index = 0; index < spaceBackgroundLayer1.size(); ++index) {
					//remove any tiles not surrounding player
					if (spaceBackgroundLayer1.get(index).tileX < newTile.x - surround || spaceBackgroundLayer1.get(index).tileX > newTile.x + surround 
							|| spaceBackgroundLayer1.get(index).tileY < newTile.y - surround || spaceBackgroundLayer1.get(index).tileY > newTile.y + surround) {
					
						spaceBackgroundLayer1.remove(index);
						
						index = -1;//reset search because removing elements changes items in array
						if (index >= spaceBackgroundLayer1.size()) {
							continue;
						}
					}
				}

				//load new tiles---------------------------------------------------------
				for (int tX = (int)newTile.x - surround; tX <= (int)newTile.x + surround; tX++) {
					for (int tY = (int)newTile.y - surround; tY <= (int)newTile.y + surround; tY++) {
						boolean exists = false; //is the tile already loaded
						for (int index = 0; index < spaceBackgroundLayer1.size() && !exists; ++index) {
							if (spaceBackgroundLayer1.get(index).tileX == tX && spaceBackgroundLayer1.get(index).tileY == tY) {					
								exists = true;
							}
						}
						if (!exists) {
							spaceBackgroundLayer1.add(new SpaceBackgroundTile(tX, tY, backgroundDepth1, tileSize));
						}
					}
				}

			}
			
			currentCenterTile = newTile;			
			checkTileCurrTime = checkTileTimer;
		}
		
		
		//adjust zoom
		zoomCamera(delta);
		
		//TODO: move into input
		if (Gdx.input.isKeyPressed(Keys.LEFT_BRACKET)) {
			cam.rotate(5f * delta);
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT_BRACKET)) {
			cam.rotate(-5f * delta);
		}
	}

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
