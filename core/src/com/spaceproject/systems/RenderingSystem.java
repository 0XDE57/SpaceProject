package com.spaceproject.systems;

import java.util.Comparator;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.spaceproject.SpaceBackgroundTile;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;

public class RenderingSystem extends IteratingSystem {
		
	private Array<Entity> renderQueue; //array of entities to render
	private Comparator<Entity> comparator; //for sorting render order
	
	//rendering
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

	
	@SuppressWarnings("unchecked")
	public RenderingSystem() {
		super(Family.all(TransformComponent.class, TextureComponent.class).get());
		
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
		cam = new OrthographicCamera();
		float aspectRatio = Gdx.graphics.getHeight() / Gdx.graphics.getWidth();	
		viewport = new ExtendViewport(WORLDHEIGHT * aspectRatio, WORLDHEIGHT, cam);
		viewport.apply();
		
		batch = new SpriteBatch();
		
		//set vsync off for development, on by default
		toggleVsync();
	
	}

	
	@Override
	public void update(float delta) {
		super.update(delta); //adds entities to render queue
			
		//clear screen with color based on camera position
		Vector3 color = backgroundColor();
		Gdx.gl20.glClearColor(color.x, color.y, color.z, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//sort render order of entities
		renderQueue.sort(comparator); 
		
		//update camera and projection
		cam.update();
		batch.setProjectionMatrix(cam.combined); 
		
		//draw
		batch.begin();
		
		//render background tiles (stars)
		for (SpaceBackgroundTile tile : LoadingSystem.getTiles()) {
			//draw = (tile position + (cam position - center of tile)) * depth			
			float drawX = tile.x + (cam.position.x - (tile.size/2)) * tile.depth;
			float drawY = tile.y + (cam.position.y - (tile.size/2)) * tile.depth;			
			
			batch.draw(tile.tex, drawX, drawY);
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
		if (Gdx.input.isKeyPressed(Keys.LEFT_BRACKET)) {
			cam.rotate(5f * delta);
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT_BRACKET)) {
			cam.rotate(-5f * delta);
		}
	}


	/**
	 * Return color based on camera position
	 * @return red in x, green in y, blue in z
	 */
	private Vector3 backgroundColor() {
		float maxColor = 0.12f;
		float ratio = 0.00001f;
		float green = Math.abs(cam.position.x * ratio);
		float blue = Math.abs(cam.position.y * ratio);
		if ((int)(green / maxColor) % 2 == 0) {
			green %= maxColor;
		} else {
			green = maxColor - green % maxColor;
		}
		if ((int)(blue / maxColor) % 2 == 1) {
			blue %= maxColor;
		} else {
			blue = maxColor - blue % maxColor;
		}
		float red = blue+green;
		Vector3 color = new Vector3(red, green + (0.15f-red)+0.05f, blue + (0.15f-red));
		return color;
	}

	private void zoomCamera(float delta) {
		if (cam.zoom != zoomTarget) {
			float scaleSpeed = 3 * delta;
			//zoom in/out
			cam.zoom += (cam.zoom < zoomTarget) ? scaleSpeed : -scaleSpeed;

			//if zoom is close enough, just set it to target
			if (Math.abs(cam.zoom - zoomTarget) < 0.1) {
				cam.zoom = zoomTarget;
				System.out.println("zoom: " + cam.zoom);
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
	}

	
	public static OrthographicCamera getCam() {
		return cam;
	}

	//resize viewport. called from screen resize
	public void resize(int width, int height) {
		viewport.update(width, height);
	}

	

}
