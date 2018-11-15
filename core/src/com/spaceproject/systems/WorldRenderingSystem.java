package com.spaceproject.systems;

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
import com.spaceproject.SpaceProject;
import com.spaceproject.Tile;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.generation.noise.NoiseBuffer;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;

import java.util.ArrayList;
import java.util.Comparator;

public class WorldRenderingSystem extends IteratingSystem implements Disposable {
		
	// rendering
	private OrthographicCamera cam;
	private SpriteBatch batch;

	// array of entities to render
	private Array<Entity> renderQueue = new Array<Entity>();
	// render order. sort by depth, z axis determines what order to draw
	private Comparator<Entity> comparator = new Comparator<Entity>() {
		@Override
		public int compare(Entity entityA, Entity entityB) {
			return (int) Math.signum(Mappers.transform.get(entityB).zOrder
					- Mappers.transform.get(entityA).zOrder);
		}
	};
	
	private ArrayList<Tile> tiles = Tile.defaultTiles;

	private long seed;
	private NoiseBuffer noise;

	private int surround; //how many tiles to draw around the camera

	private Texture tileTex = TextureFactory.createTile(new Color(1f, 1f, 1f, 1f));
	
	public WorldRenderingSystem(Entity planetEntity) {
		this(planetEntity, MyScreenAdapter.cam, MyScreenAdapter.batch);
	}
	
	public WorldRenderingSystem(Entity planetEntity, OrthographicCamera camera, SpriteBatch spriteBatch) {
		super(Family.all(TransformComponent.class, TextureComponent.class).get());
		
		cam = camera;
		batch = spriteBatch;

		surround = 30;//TODO: split into surrondX/Y, change to be calculated by tileSize and window height/width

		SeedComponent seedComp = planetEntity.getComponent(SeedComponent.class);

		seed = seedComp.seed;
		loadMap();

	}

	private void loadMap() {
		//use cached noise
		//TODO: if not cached and if not in process of being generated, only then generate, but this should probably never happen

		NoiseBuffer newNoise = GameScreen.universe.getNoiseForSeed(seed);
		if (newNoise != null) {
			noise = newNoise;
			//mapSize = newNoise.heightMap.length * SpaceProject.worldcfg.tileSize;
		}

		if (noise == null) {
			System.out.println("no map found");
		} else {
			System.out.println("map found");
		}
	}
	
	


	@Override
	public void update(float delta) {
		super.update(delta); //adds entities to render queue
			
		//clear screen
		Gdx.gl20.glClearColor(0, 0, 0, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		batch.begin();

		if (noise == null) {
			loadMap();
		} else {
			//render background tiles
			drawTiles();
		}

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
		int tileSize = SpaceProject.worldcfg.tileSize;
		int centerX = (int) (cam.position.x / tileSize);
		int centerY = (int) (cam.position.y / tileSize);

		// subtract 1 from tile position if less than zero to account for -1/n = 0
		if (cam.position.x < 0) --centerX;		
		if (cam.position.y < 0) --centerY;
		
		for (int tileY = centerY - surround; tileY <= centerY + surround; tileY++) {
			for (int tileX = centerX - surround; tileX <= centerX + surround; tileX++) {
				
				//wrap tiles when position is outside of map
				int tX = tileX % noise.heightMap.length;
				int tY = tileY % noise.heightMap.length;
				if (tX < 0) tX += noise.heightMap.length;
				if (tY < 0) tY += noise.heightMap.length;
							
				//render tile
				batch.setColor(tiles.get(noise.tileMap[tX][tY]).getColor());
				//if (tX == heightMap.length-1 || tY == heightMap.length-1) batch.setColor(Color.BLACK);
				batch.draw(tileTex,
						tileX * tileSize,
						tileY * tileSize,
						tileSize, tileSize);
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
	}

}
