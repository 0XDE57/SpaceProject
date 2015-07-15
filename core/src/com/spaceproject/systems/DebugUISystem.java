package com.spaceproject.systems;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.FontFactory;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;

public class DebugUISystem extends IteratingSystem {

	private SpriteBatch batch;
	private ShapeRenderer shape;
	private BitmapFont font;
	
	private Array<Entity> objects;
	private ComponentMapper<TransformComponent> transformMap;
	private ComponentMapper<BoundsComponent> boundsMap;
	private ComponentMapper<OrbitComponent> orbitMap;
	
	//config
	private boolean drawDebugUI = true;
	private boolean drawFPS = true;
	private boolean drawComponentList = false;
	private boolean drawBounds = false;
	private boolean drawOrbitPath = true;
	
	private Matrix4 projectionMatrix = new Matrix4();
	
	
	@SuppressWarnings("unchecked")
	public DebugUISystem() {
		super(Family.all(TransformComponent.class).get());
		
		transformMap = ComponentMapper.getFor(TransformComponent.class);
		boundsMap = ComponentMapper.getFor(BoundsComponent.class);
		orbitMap = ComponentMapper.getFor(OrbitComponent.class);
		
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
		batch = new SpriteBatch();
		shape = new ShapeRenderer();
		
		objects = new Array<Entity>();		
		
	}
	
	@Override
	public void update(float delta) {
		//toggle debug
		if (Gdx.input.isKeyJustPressed(Keys.F3)) {
			drawDebugUI = !drawDebugUI;
			System.out.println("DEBUG UI: " + drawDebugUI);
		}
		
		//don't update if we aren't drawing
		if (!drawDebugUI) return;
		
		//toggle components
		if (Gdx.input.isKeyJustPressed(Keys.NUMPAD_1)) {
			drawComponentList = !drawComponentList;
			System.out.println("[debug] draw component list: " + drawComponentList);
		}
		
		//toggle bounds		
		if (Gdx.input.isKeyJustPressed(Keys.NUMPAD_2)) {
			drawBounds = !drawBounds;
			System.out.println("[debug] draw bounds: " + drawBounds);
		}
		
		//toggle fps
		if (Gdx.input.isKeyJustPressed(Keys.NUMPAD_3)) {
			drawFPS = !drawFPS;
			System.out.println("[debug] draw FPS: " + drawFPS);
		}
			
		//toggle orbit circle
		if (Gdx.input.isKeyJustPressed(Keys.NUMPAD_4)) {
			drawOrbitPath = !drawOrbitPath;
			System.out.println("[debug] draw orbit path: " + drawOrbitPath);
		}
		
		
		super.update(delta);
		
		//set projection matrix so things render using correct coordinates
		//TODO: only needs to be called when screen size changes
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); 
		batch.setProjectionMatrix(projectionMatrix);
		shape.setProjectionMatrix(projectionMatrix);
		
		
		
		//draw filled shapes--------------------------------------------
		//enable blending for transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		
		shape.begin(ShapeType.Filled);
			
		//draw light background for text visibility
		if (drawComponentList) drawComponentListBack();
		
		//draw the bounding box (collision detection) for collidables
		if (drawBounds) drawBounds();
		
		
		
		shape.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);
		
		//draw non-filled shapes--------------------------------------
		shape.begin(ShapeType.Line);
		
		// draw ring to visualize orbit path
		if (drawOrbitPath) drawOrbitPath();
		
		shape.end();
		
		
		//batch------------------------------------------------
		batch.begin();
		
		//print debug menu
		boolean drawMenu = true;
		if (drawMenu){
			font.setColor(1, 1, 1, 1);
			font.draw(batch, "---DEBUG [F3]---", 15, Gdx.graphics.getHeight() - 45);
			font.draw(batch, "[NUM1] Draw Component List: " + drawComponentList, 15, Gdx.graphics.getHeight() - 60);
			font.draw(batch, "[NUM2] Draw Bounds: " + drawBounds, 15, Gdx.graphics.getHeight() - 75);
			font.draw(batch, "[NUM3] Draw FPS: " + drawFPS, 15, Gdx.graphics.getHeight() - 90);
			font.draw(batch, "[NUM4] Draw Orbit Path: " + drawOrbitPath, 15, Gdx.graphics.getHeight() - 105);
		}
		
		//draw frames per second
		if (drawFPS) drawFPS();
		//TODO: entity and component count (see ashley tests)
		
		//draw components on entity
		if (drawComponentList) drawComponentList();

		batch.end();
	
		
		objects.clear();
		
	}

	/** draw orbit path, a ring to visualize objects orbit*/
	private void drawOrbitPath() {
		shape.setColor(1f, 1f, 1, 1);
		for (Entity entity : objects) {
			OrbitComponent orbit = orbitMap.get(entity);
			if (orbit != null && orbit.parent != null) {
				TransformComponent parentPos = transformMap.get(orbit.parent);
				TransformComponent entityPos = transformMap.get(entity);
				Vector3 screenPos1 = RenderingSystem.getCam().project(new Vector3(parentPos.pos.x, parentPos.pos.y, 0));
				Vector3 screenPos2 = RenderingSystem.getCam().project(new Vector3(entityPos.pos.x, entityPos.pos.y, 0));
				float distance = (float) Math.sqrt(Math.pow(screenPos1.x - screenPos2.x, 2) + Math.pow(screenPos1.y - screenPos2.y, 2));
				shape.circle(screenPos1.x, screenPos1.y, distance);
				shape.line(screenPos1.x, screenPos1.y, screenPos2.x, screenPos2.y);
			}
		}
	}
	
	/** draw bounding boxes (hitbox/collision detection) */
	private void drawBounds() {
		shape.setColor(1, 0.5f, 1, 0.5f);
		for (Entity entity : objects) { 
			BoundsComponent bounds = boundsMap.get(entity);
			//TODO fix projection on window resize, probably the bounds width and height
			Vector3 screenPos = RenderingSystem.getCam().project(new Vector3(bounds.bounds.x, bounds.bounds.y, 0));
			shape.rect(screenPos.x, screenPos.y, bounds.bounds.width, bounds.bounds.height);
		}
		
	}

	/** draw Frames in top left corner */
	private void drawFPS() {
		font.setColor(1,1,1,1);
		font.draw(batch, Integer.toString(Gdx.graphics.getFramesPerSecond()), 15, Gdx.graphics.getHeight()- 15);
	}
	
	/**  Draw background for easier text reading */
	private void drawComponentListBack() {
		shape.setColor(0.5f, 0.5f, 0.5f, 0.6f);
		int padding = 5;
		//for each entity draw a clear box 
		for (Entity entity : objects) {
			TransformComponent transform = transformMap.get(entity);
			Vector3 screenPos = RenderingSystem.getCam().project(transform.pos.cpy());
			//draw rectangle with size relative to number of components and text size (20). 
			//200 box width - magic number assuming no component name will be that long 
			shape.rect(screenPos.x-padding, screenPos.y+padding, 200, ((-entity.getComponents().size() - 1) * 20) - padding);
		}
	}

	/**  Draw Entity ID, position and list of components attached. */
	private void drawComponentList() {
		font.setColor(1, 1, 1, 1);
		for (Entity entity : objects) {
			//get entities position and list of components
			TransformComponent t = transformMap.get(entity);
			ImmutableArray<Component> components = entity.getComponents();
			
			//use Vector3.cpy() to project only the position and avoid modifying projection matrix for all coordinates
			Vector3 screenPos = RenderingSystem.getCam().project(t.pos.cpy());
			//print current ID and position in world and a list of all components
			font.draw(batch, "ID: " + entity.getId() + " (" + Math.round(t.pos.x) + "," + Math.round(t.pos.y) + ")", screenPos.x, screenPos.y);
			int nextLine = 20;
			for (int curComp = 0; curComp < components.size(); curComp++) {
				font.draw(batch, "[" + components.get(curComp).getClass().getSimpleName() + "]", screenPos.x, screenPos.y - nextLine * (curComp + 1));
			}
			
		}
		
	}

	@Override 
	public void processEntity(Entity entity, float deltaTime) {
		objects.add(entity);
	}
	
}
