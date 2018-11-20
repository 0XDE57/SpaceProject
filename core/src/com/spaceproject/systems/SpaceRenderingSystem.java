package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceBackgroundTile;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;

import java.util.ArrayList;
import java.util.Comparator;

public class SpaceRenderingSystem extends IteratingSystem implements Disposable {

	//rendering
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
	
	public SpaceRenderingSystem() {
		this(MyScreenAdapter.cam, MyScreenAdapter.batch);
	}
	
	public SpaceRenderingSystem(OrthographicCamera camera, SpriteBatch spriteBatch) {
		super(Family.all(TransformComponent.class).one(TextureComponent.class, Sprite3DComponent.class).get());

					
		cam = camera;
		this.spriteBatch = spriteBatch;

		modelBatch = new ModelBatch();
	}
	
	@Override
	public void update(float delta) {
		super.update(delta); //adds entities to render queue
			
		//clear screen with color based on camera position
		Color color = backgroundColor(cam);
		Gdx.gl20.glClearColor(color.r, color.g, color.b, 1);
		Gdx.gl.glClearDepthf(1f);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | Gdx.gl20.GL_DEPTH_BUFFER_BIT);

		spriteBatch.begin();
		
		//draw background tiles (stars)
		drawParallaxTiles();
		
		//draw game objects
		drawEntities();
		
		spriteBatch.end();


		modelBatch.begin(cam);
		for (Entity entity : renderQueue3D) {
			Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
			TransformComponent t = Mappers.transform.get(entity);


			//System.out.println(t.rotation);
			//sprite3D.sprite.worldTransform.setToRotationRad(Vector3.Z, t.rotation);
			//System.out.println(sprite3D.sprite.worldTransform);
			//sprite3D.renderable.worldTransform.rotateRad(Vector3.Z, t.rotation);


			if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
				//sprite3D.roll += 50 * delta;
				//sprite3D.sprite.worldTransform.rotate(Vector3.X, sprite3D.roll);
				sprite3D.renderable.angle += 15*delta;
				//t.zOrder+=1;
			}
			if (Gdx.input.isKeyPressed(Input.Keys.E)) {
				//sprite3D.roll -= 50 * delta;
				//sprite3D.sprite.worldTransform.rotate(Vector3.X, sprite3D.roll);
				sprite3D.renderable.angle -= 15*delta;
				//t.zOrder-=1;
			}
			if (Gdx.input.isKeyPressed(Input.Keys.R)) {
				sprite3D.renderable.angle += (float)Math.PI;
			}




			//sprite3D.sprite.worldTransform.rotate(Vector3.X, sprite3D.roll);

			//sprite3D.sprite.worldTransform.setTranslation(0,-70,-50);

			sprite3D.renderable.position.set(t.pos.x, t.pos.y, -50);
			sprite3D.renderable.rotation.set(Vector3.X, sprite3D.renderable.angle*MathUtils.radDeg);
			//sprite3D.renderable.rotation.set(Vector3.X, t.rotation);// = (sprite3D.renderable.angle + 90 * delta) % 360;
			sprite3D.renderable.update();
			//System.out.println(MyMath.round(sprite3D.renderable.angle * MathUtils.radDeg,1));
			//sprite3D.renderable.worldTransform.set(sprite3D.renderable.position, sprite3D.renderable.rotation);

			modelBatch.render(sprite3D.renderable);
		}
		renderQueue3D.clear();
		modelBatch.end();

	
	}

	private void drawEntities() {
		//sort render order of entities
		renderQueue.sort(comparator);
		
		//draw all textures
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
					   0, 0, (int)width, (int)height, false, false);
		}
		
		renderQueue.clear();
	}
	
	private void drawParallaxTiles() {
		ArrayList<SpaceBackgroundTile> tiles = SpaceParallaxSystem.getTiles();
		if (tiles == null){
			return;
		}

		for (SpaceBackgroundTile tile : tiles) {
			//draw = (tile position + (cam position - center of tile)) * depth			
			float drawX = tile.x + (cam.position.x - (tile.size/2)) * tile.depth;
			float drawY = tile.y + (cam.position.y - (tile.size/2)) * tile.depth;			
			//batch.draw(tile.tex, drawX, drawY);
			
			//draw texture
			float width = tile.tex.getWidth();
			float height = tile.tex.getHeight();
			spriteBatch.draw(tile.tex, drawX, drawY,
					   0,0,
					   width, height,
					   tile.scale, tile.scale,
					   0, 0, 0, (int)width, (int)height, false, false);
		}
	}


	/**
	 * Return color based on camera position
	 * @return color
	 */
	private static Color backgroundColor(Camera cam) {
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
		Color color = new Color(red, green + (maxColor-red)+0.2f, blue + (maxColor-red)+0.1f,1);
		return color;
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
	
	@Override
	public void dispose() {
		
		//dispose of all textures
		for (Entity entity : renderQueue) {
			TextureComponent tex = Mappers.texture.get(entity);	
			if (tex.texture != null)
				tex.texture.dispose();
		}
		
		for (SpaceBackgroundTile tile : SpaceParallaxSystem.getTiles()) {
			tile.tex.dispose();
		}
		
		//batch.dispose();
		/*
		 * EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x0000000054554370,
		 * pid=5604, tid=2364 
		 * Problematic frame: C [atio6axx.dll+0x3c4370]
		 */
	}

}
