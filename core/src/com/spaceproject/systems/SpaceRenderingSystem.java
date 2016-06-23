package com.spaceproject.systems;

import java.util.Comparator;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.spaceproject.SpaceBackgroundTile;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyScreenAdapter;

public class SpaceRenderingSystem extends IteratingSystem implements Disposable, InputProcessor {
	
	private Array<Entity> renderQueue; //array of entities to render
	private Comparator<Entity> comparator; //for sorting render order
	
	//rendering
	private static OrthographicCamera cam;
	private static SpriteBatch batch;
	
	boolean animateLanding;
	
	public SpaceRenderingSystem() {
		this(MyScreenAdapter.cam, MyScreenAdapter.batch);
	}
	
	public SpaceRenderingSystem(OrthographicCamera camera, SpriteBatch spriteBatch) {
		super(Family.all(TransformComponent.class, TextureComponent.class).get());
		
		//set this as input processor for mouse wheel scroll events
		Gdx.input.setInputProcessor(this);
			
		cam = camera;
		batch = spriteBatch;
		
		renderQueue = new Array<Entity>();
		
		//sort by depth, z axis determines what order to draw 
		comparator = new Comparator<Entity>() {
			@Override
			public int compare(Entity entityA, Entity entityB) {
				return (int)Math.signum(Mappers.transform.get(entityB).pos.z -
										Mappers.transform.get(entityA).pos.z);
			}
		};

		
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

		//draw
		batch.begin();
		
		//render background tiles (stars)
		for (SpaceBackgroundTile tile : SpaceLoadingSystem.getTiles()) {
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
	
	}


	/**
	 * Return color based on camera position
	 * @return red in x, green in y, blue in z
	 */
	private static Vector3 backgroundColor() {
		//still playing with these values to get the right feel/intensity of color...
		float maxColor = 0.25f;
		float ratio = 0.00001f;
		float green = Math.abs(cam.position.x * ratio);
		float blue = Math.abs(cam.position.y * ratio);
		//green based on x position. range amount of green between 0 and maxColor
		if ((int)(green / maxColor) % 2 == 0) {
			green %= maxColor;
		} else {
			green = maxColor - green % maxColor;
		}
		//blue based on y position. range amount of blue between 0 and maxColor
		if ((int)(blue / maxColor) % 2 == 0) {
			blue %= maxColor;
		} else {
			blue = maxColor - blue % maxColor;
		}
		//red is combination of blue and green
		float red = blue+green;
		Vector3 color = new Vector3(red, green + (maxColor-red)+0.15f, blue + (maxColor-red)+0.1f);
		return color;
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
		
		for (SpaceBackgroundTile tile : SpaceLoadingSystem.getTiles()) {
			tile.tex.dispose();
		}
		
		//batch.dispose();//crashes: 
		/*
		EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x0000000054554370, pid=5604, tid=2364
		Problematic frame:
	 	C  [atio6axx.dll+0x3c4370]
		 */
	}

	@Override
	public boolean keyDown(int keycode) { return false; }

	@Override
	public boolean keyUp(int keycode) { return false; }

	@Override
	public boolean keyTyped(char character) { return false; }

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

	@Override
	public boolean mouseMoved(int screenX, int screenY) { return false; }

	@Override
	public boolean scrolled(int amount) {
		MyScreenAdapter.setZoomTarget(cam.zoom += amount/2f);
		return false;
	}

}
