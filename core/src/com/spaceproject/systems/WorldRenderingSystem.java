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
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.generation.noise.NoiseBuffer;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Tile;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;

import java.util.ArrayList;
import java.util.Comparator;

public class WorldRenderingSystem extends IteratingSystem implements IRequireGameContext {
    
    // rendering
    private OrthographicCamera cam;
    private SpriteBatch spriteBatch;
    private ModelBatch modelBatch;
    
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
    
    private Array<Entity> renderQueue3D = new Array<Entity>();
    
    private ArrayList<Tile> tiles = Tile.defaultTiles;
    
    private NoiseBuffer noise;
    
    private int surround; //how many tiles to draw around the camera
    
    private Texture tileTex = TextureFactory.createTile(new Color(1f, 1f, 1f, 1f));
    WorldConfig worldCFG;
    
    private boolean debugShowEdgeTile = false;
    
    public WorldRenderingSystem() {
        super(Family.all(TransformComponent.class).one(TextureComponent.class, Sprite3DComponent.class).get());
        worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
        this.cam = GameScreen.cam;
        this.spriteBatch = GameScreen.batch;
        modelBatch = new ModelBatch();
        
        surround = 30;//TODO: split into surrondX/Y, change to be calculated by tileSize and window height/width
    }
    
    @Override
    public void initContext(GameScreen gameScreen) {
        SeedComponent seedComp = gameScreen.getCurrentPlanet().getComponent(SeedComponent.class);
        
        loadMap(seedComp.seed);
    }
    
    private void loadMap(long seed) {
        //TODO: rendering system should not be responsible for loading map, create world loading system.
        long time = System.currentTimeMillis();
        long timeout = 10000;
        
        do {
            noise = GameScreen.noiseManager.getNoiseForSeed(seed);
            
            if ((System.currentTimeMillis() - time) > timeout) {
                Gdx.app.log(this.getClass().getSimpleName(), "could not find seed for noise: " + seed);
                //TODO: if not cached and if not in process of being generated, only then generate. but this should probably never happen?
                //GameScreen.noiseManager.loadOrCreateNoiseFor(seed, PlanetComponent);
            }
        } while (noise == null);
    }
    
    @Override
    public void update(float delta) {
        super.update(delta); //adds entities to render queue
        
        //clear screen
        Gdx.gl20.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClearDepthf(1f);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        
        
        //render background tiles
        drawTiles(worldCFG.tileSize);
        
        
        //draw game objects
        drawEntities();
        
        spriteBatch.end();
        
        modelBatch.begin(cam);
        draw3DRenderables(delta);
        modelBatch.end();
    }
    
    private void drawEntities() {
        spriteBatch.setColor(Color.WHITE);
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
            spriteBatch.draw(tex.texture, (t.pos.x - originX), (t.pos.y - originY),
                    originX, originY,
                    width, height,
                    tex.scale, tex.scale,
                    MathUtils.radiansToDegrees * t.rotation,
                    0, 0, (int) width, (int) height, false, false);
        }
        renderQueue.clear();
    }
    
    
    private void drawTiles(int tileSize) {
        // calculate tile that the camera is in
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
                spriteBatch.setColor(tiles.get(noise.tileMap[tX][tY]).getColor());
                if (debugShowEdgeTile) {
                    if (tX == noise.heightMap.length - 1 || tY == noise.heightMap.length - 1)
                        spriteBatch.setColor(Color.BLACK);
                }
                spriteBatch.draw(tileTex,
                        tileX * tileSize,
                        tileY * tileSize,
                        tileSize, tileSize);
            }
        }
    }
    
    private void draw3DRenderables(float delta) {
        for (Entity entity : renderQueue3D) {
            Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
            TransformComponent t = Mappers.transform.get(entity);


			/*
			if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
				sprite3D.renderable.angle += 15*delta;
			}
			if (Gdx.input.isKeyPressed(Input.Keys.E)) {
				sprite3D.renderable.angle -= 15*delta;
			}
			if (Gdx.input.isKeyPressed(Input.Keys.R)) {
				sprite3D.renderable.angle += (float)Math.PI;
			}
			System.out.println(sprite3D.renderable.angle * MathUtils.radDeg);
			*/

			/*
			//TODO: would prefer to use this method rather than direct world transform, problems:
			//		set() seems to overwrite previous rotation only applying last called set
			//		setEulerAnglesRad() seems to apply pitch and yaw in the opposite order we desire
			sprite3D.renderable.position.set(t.pos.x, t.pos.y, -50);
			//sprite3D.renderable.rotation.set(Vector3.X, MathUtils.radDeg * sprite3D.renderable.angle);//"roll"
			//sprite3D.renderable.rotation.set(Vector3.Z, MathUtils.radDeg * t.rotation);//"orientation facing"
			sprite3D.renderable.rotation.setEulerAnglesRad(0, sprite3D.renderable.angle, t.rotation);//this applies in the wrong order resulting in funny rotation
			sprite3D.renderable.update();
			*/
            
            
            //TODO: the switch to renderables from textures seems to have a performance impact. currently it's a mesh and texture per entity.
            //see https://xoppa.github.io/blog/a-simple-card-game/#reduce-the-number-of-render-calls
            sprite3D.renderable.worldTransform.setToRotation(Vector3.Z, MathUtils.radDeg * t.rotation);
            sprite3D.renderable.worldTransform.rotate(Vector3.X, MathUtils.radDeg * sprite3D.renderable.angle);
            sprite3D.renderable.worldTransform.setTranslation(t.pos.x, t.pos.y, -50);
            sprite3D.renderable.worldTransform.scale(sprite3D.renderable.scale.x, sprite3D.renderable.scale.y, sprite3D.renderable.scale.z);
            
            modelBatch.render(sprite3D.renderable);
        }
        renderQueue3D.clear();
    }
    
    @Override
    public void processEntity(Entity entity, float deltaTime) {
        if (Mappers.texture.get(entity) != null) {
            //Add entities to render queue
            renderQueue.add(entity);
        } else {
            renderQueue3D.add(entity);
        }
    }
    
}
