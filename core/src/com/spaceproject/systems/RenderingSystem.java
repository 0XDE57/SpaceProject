package com.spaceproject.systems;

import java.util.Comparator;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.spaceproject.BackgroundStar;
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
	
	private Array<BackgroundStar> backgroundStarsLayer1 = new Array<BackgroundStar>();
	private Array<BackgroundStar> backgroundStarsLayer2 = new Array<BackgroundStar>();
	private Array<BackgroundStar> backgroundStarsLayer3 = new Array<BackgroundStar>();
	
	Texture starTexture;
	
	Engine engine;
	
	
	
	@SuppressWarnings("unchecked")
	public RenderingSystem(Engine engine) {
		super(Family.all(TransformComponent.class, TextureComponent.class).get());
	
		this.engine = engine;
		
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
		
		//generate stars
		for (int i = 0; i < 30000; i++) {		
			backgroundStarsLayer1.add(new BackgroundStar((float)MathUtils.random(-5000, 5000), (float)MathUtils.random(-5000, 5000)));
			backgroundStarsLayer2.add(new BackgroundStar((float)MathUtils.random(-5000, 5000), (float)MathUtils.random(-5000, 5000)));
			backgroundStarsLayer3.add(new BackgroundStar((float)MathUtils.random(-5000, 5000), (float)MathUtils.random(-5000, 5000)));
		}
		
		//star texture pixel
		Pixmap pixmap = new Pixmap(1, 1, Format.RGB888);
		pixmap.setColor(1,1,1,1);
		pixmap.fill();
		starTexture = new Texture(pixmap);
		pixmap.dispose();
	}
	/*
	@Override
	public void addedToEngine(Engine engine) {
		//this.engine = engine;
	}	*/
	

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

		//render stars
		
		for (BackgroundStar star : backgroundStarsLayer3) {
			batch.draw(starTexture, (float) star.x + (cam.position.x * 0.9f), (float) star.y + (cam.position.y * 0.9f));
		}
		for (BackgroundStar star : backgroundStarsLayer2) {
			batch.draw(starTexture, (float) star.x + (cam.position.x * 0.6f), (float) star.y + (cam.position.y * 0.6f));
		}
		for (BackgroundStar star : backgroundStarsLayer1) {
			batch.draw(starTexture, (float) star.x + (cam.position.x * 0.3f), (float) star.y + (cam.position.y * 0.3f));
		}
		
		//render all textures
		for (Entity entity : renderQueue) {
			TextureComponent tex = textureMap.get(entity);
		
			if (tex.texture == null) continue;
		
			float width = tex.texture.getWidth();
			float height = tex.texture.getHeight();
			float originX = width * 0.5f; //center 
			float originY = height * 0.5f; //center
			
			TransformComponent t = transformMap.get(entity);
			Vector3 local = CameraSystem.getLocalPosition(t);
			//draw texture
			//batch.draw(tex.texture, (t.pos.x - originX), (t.pos.y - originY),
			batch.draw(tex.texture, (local.x - originX), (local.y - originY),
					   originX, originY,
					   width, height,
					   tex.scale, tex.scale,
					   MathUtils.radiansToDegrees * t.rotation, 
					   0, 0, (int)width, (int)height, false, false);
		}
		batch.end();
		
		renderQueue.clear();
	
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
