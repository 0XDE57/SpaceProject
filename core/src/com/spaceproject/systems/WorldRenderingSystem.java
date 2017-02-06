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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.Tile;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyScreenAdapter;
import com.spaceproject.utility.NoiseGen;

public class WorldRenderingSystem extends IteratingSystem implements Disposable {
		
	// rendering
	private static OrthographicCamera cam;
	private static SpriteBatch batch;

	// array of entities to render
	private Array<Entity> renderQueue = new Array<Entity>();
	// render order. sort by depth, z axis determines what order to draw
	private Comparator<Entity> comparator = new Comparator<Entity>() {
		@Override
		public int compare(Entity entityA, Entity entityB) {
			return (int) Math.signum(Mappers.transform.get(entityB).pos.z 
					- Mappers.transform.get(entityA).pos.z);
		}
	};
	
	private ArrayList<Tile> tiles = Tile.defaultTiles;
	
	private float[][] heightMap;//height of tile
	private int[][] tileMap;//index of tile
		
	private int tileSize; //render size of tiles
	private int surround; //how many tiles to draw around the camera

	static Texture tileTex = TextureFactory.createTile(new Color(1f, 1f, 1f, 1f));
	
	public WorldRenderingSystem(PlanetComponent planet) {
		this(planet, MyScreenAdapter.cam, MyScreenAdapter.batch);
	}
	
	public WorldRenderingSystem(PlanetComponent planet, OrthographicCamera camera, SpriteBatch spriteBatch) {
		super(Family.all(TransformComponent.class, TextureComponent.class).get());
		
		cam = camera;
		batch = spriteBatch;

		tileSize = 32;
		surround = 30;	
				
		loadMap(planet);
	
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
		
		batch.begin();
		
		//render background tiles
		drawTiles();
		
		//draw game objects
		drawEntities();
		
		batch.end();
	}

	private void drawEntities() {
		batch.setColor(Color.WHITE);	
		//sort render order of entities
		renderQueue.sort(comparator); 
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
		renderQueue.clear();
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

	}
	
	@Override
	public void processEntity(Entity entity, float deltaTime) {
		//Add entities to render queue
		renderQueue.add(entity);
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
