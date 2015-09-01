package com.spaceproject.systems;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.CustomIteratingSystem;
import com.spaceproject.FontFactory;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;

public class DebugUISystem extends CustomIteratingSystem {

	private SpriteBatch batch;
	private ShapeRenderer shape;
	private BitmapFont font;
	
	private Array<Entity> objects;
	private ComponentMapper<TransformComponent> transformMap;
	private ComponentMapper<MovementComponent> movementMap;
	private ComponentMapper<BoundsComponent> boundsMap;
	private ComponentMapper<OrbitComponent> orbitMap;
	
	//config
	private boolean drawDebugUI = true;
	private boolean drawMenu = false;
	private boolean drawFPS = true;
	private boolean drawComponentList = false;
	private boolean drawPos = false;
	private boolean drawBounds = false;
	private boolean drawOrbitPath = false;
	private boolean drawVectors = false;
	
	private Matrix4 projectionMatrix = new Matrix4();
	
	//entity and component counting
	private float countTimer = 50;
	private float curCountTime = countTimer;
	private int entityCount = 0;
	private int componentCount = 0;
	
	@SuppressWarnings("unchecked")
	public DebugUISystem() {
		super(Family.all(TransformComponent.class).get());
		
		transformMap = ComponentMapper.getFor(TransformComponent.class);
		movementMap = ComponentMapper.getFor(MovementComponent.class);
		boundsMap = ComponentMapper.getFor(BoundsComponent.class);
		orbitMap = ComponentMapper.getFor(OrbitComponent.class);
		
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
		batch = new SpriteBatch();
		shape = new ShapeRenderer();
		
		objects = new Array<Entity>();		
		
	}
	
	@Override
	public void update(float delta) {
		//check key presses
		updateKeyToggles();
		
		//don't update if we aren't drawing
		if (!drawDebugUI) return;				
		super.update(delta);
		
			
		//update timer
		curCountTime -= 100 * delta;
		
		
		//set projection matrix so things render using correct coordinates
		//TODO: only needs to be called when screen size changes
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); 
		batch.setProjectionMatrix(projectionMatrix);
		shape.setProjectionMatrix(projectionMatrix);
		
		
		
		//draw filled shapes///////////////////////////////////////////////
		//enable blending for transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		
		shape.begin(ShapeType.Filled);
			
		//draw light background for text visibility
		if (drawComponentList) drawComponentListBack();
			
		shape.end();
		Gdx.gl.glDisable(GL20.GL_BLEND); //disable blending
		
		
		
		//draw non-filled shapes//////////////////////////////////////////
		shape.begin(ShapeType.Line);
		
		//draw vector to visualize speed and direction
		if (drawVectors) drawMovementVectors();
		
		// draw ring to visualize orbit path
		if (drawOrbitPath) drawOrbitPath();
		
		//draw the bounding box (collision detection) for collidables
		if (drawBounds) drawBounds();
		
		shape.end();
		
		
		
		//draw batch////////////////////////////////////////////////////////
		batch.begin();
		
		//print debug menu
		if (drawMenu)  drawDebugMenu();
			
		//draw frames per second and entity count
		if (drawFPS) drawFPS();
		
		//draw entity position
		if (drawPos) drawPos();
		
		//draw components on entity
		if (drawComponentList) drawComponentList();

		batch.end();
	
		
		
		objects.clear();		
	}

	/** Draw lines to represent speed and direction of entity */
	private void drawMovementVectors() {
		for (Entity entity : objects) {
			//get entities position and list of components
			TransformComponent t = transformMap.get(entity);
			MovementComponent m = movementMap.get(entity);
			if (m == null) continue;
			
			Vector3 screenPos = RenderingSystem.getCam().project(t.pos.cpy());

			//calculate vector angle and length
			float scale = 4; //how long to make vectors (higher number is shorter line)
			float length = m.velocity.len();
			float angle = getMoveAngle(m.velocity.x, m.velocity.y);
			float pointX = screenPos.x + (length / scale * MathUtils.cos(angle));
			float pointY = screenPos.y + (length / scale * MathUtils.sin(angle));
			
			//draw line to represent movement
			shape.line(screenPos.x, screenPos.y, pointX, pointY, Color.RED, Color.MAGENTA);
		}
	}

	
	//TODO move to a util/math class
	public static double getLength(float x, float y){
		return(Math.sqrt(x * x + y * y));
	}
	
	//TODO move to a util/math class
	public static float getMoveAngle(float dx, float dy) {
		float angle = (float)Math.atan2(dy, dx) * MathUtils.radiansToDegrees;
		if (angle < 0) angle += 360;
		return angle * MathUtils.degreesToRadians;
	}
	
	private void updateKeyToggles() {
		//toggle debug
		if (Gdx.input.isKeyJustPressed(Keys.F3)) {
			drawDebugUI = !drawDebugUI;
			System.out.println("DEBUG UI: " + drawDebugUI);
		}
		
		//toggle pos
		if (Gdx.input.isKeyJustPressed(Keys.NUMPAD_0)) {
			drawPos = !drawPos;
			if(drawComponentList) {
				drawComponentList = false;
			}
			System.out.println("[debug] draw pos: " + drawPos);
		}
		
		//toggle components
		if (Gdx.input.isKeyJustPressed(Keys.NUMPAD_1)) {
			drawComponentList = !drawComponentList;
			if (drawPos) {
				drawPos = false;
			}
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
		
		
		//toggle vector
		if (Gdx.input.isKeyJustPressed(Keys.NUMPAD_5)) {
			drawVectors = !drawVectors;
			System.out.println("[debug] draw vectors: " + drawVectors);
		}
		
		//toggle menu
		if (Gdx.input.isKeyJustPressed(Keys.NUMPAD_9)) {
			drawMenu = !drawMenu;
		}
	}

	/** draw menu showing items to draw and toggle keys */
	private void drawDebugMenu() {
		font.setColor(1, 1, 1, 1);
		font.draw(batch, "***DEBUG [F3]***", 15, Gdx.graphics.getHeight() - 45);
		font.draw(batch, "[NUM0] Draw Pos: " + drawPos, 15, Gdx.graphics.getHeight() - 60);
		font.draw(batch, "[NUM1] Draw Component List: " + drawComponentList, 15, Gdx.graphics.getHeight() - 75);
		font.draw(batch, "[NUM2] Draw Bounds: " + drawBounds, 15, Gdx.graphics.getHeight() - 90);
		font.draw(batch, "[NUM3] Draw FPS: " + drawFPS, 15, Gdx.graphics.getHeight() - 105);
		font.draw(batch, "[NUM4] Draw Orbit Path: " + drawOrbitPath, 15, Gdx.graphics.getHeight() - 120);
		font.draw(batch, "[NUM5] Draw Vectors: " + drawOrbitPath, 15, Gdx.graphics.getHeight() - 135);
		font.draw(batch, "[NUM9] Hide this menu.", 15, Gdx.graphics.getHeight() - 150);
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
		for (Entity entity : objects) { 
			BoundsComponent bounds = boundsMap.get(entity);		
			TransformComponent t = transformMap.get(entity);
			
			if (bounds != null) {
				//draw Axis-Aligned bounding box			
				Vector3 screenPosT = RenderingSystem.getCam().project(new Vector3(t.pos.x, t.pos.y, 0));
				Rectangle rect = bounds.poly.getBoundingRectangle();
				shape.setColor(1, 1, 0, 1);
				shape.rect(screenPosT.x - rect.width/2, screenPosT.y - rect.height/2, rect.width, rect.height);

				//draw Orientated bounding box
				Vector3 screenPos = RenderingSystem.getCam().project(new Vector3(bounds.poly.getX(), bounds.poly.getY(), 0));
				bounds.poly.setPosition(screenPos.x , screenPos.y);
				shape.setColor(1, 0, 0, 1);
				shape.polygon(bounds.poly.getTransformedVertices());
			}
			
		}
		
	}

	/** draw Frames and entity count in top left corner */
	private void drawFPS() {
		font.setColor(1,1,1,1);

		if (curCountTime < 0) {
			entityCount = engine.getEntities().size();
			componentCount = 0;
			for (Entity ent : engine.getEntities()) {
				componentCount += ent.getComponents().size();
			}
			//System.out.println("Entities: " + entityCount + " - Components: " + componentCount);
			curCountTime = countTimer;
		}
		String count = "   E: " + entityCount + " - C: " + componentCount;
		
		font.draw(batch, Integer.toString(Gdx.graphics.getFramesPerSecond()) + count, 15, Gdx.graphics.getHeight()- 15);
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
			MovementComponent m = movementMap.get(entity);
			ImmutableArray<Component> components = entity.getComponents();
			
			//use Vector3.cpy() to project only the position and avoid modifying projection matrix for all coordinates
			Vector3 screenPos = RenderingSystem.getCam().project(t.pos.cpy());
			//print current ID and position in world and a list of all components
			String vel = "";
			if (m != null) {
				vel = " ~ " + Math.round(Math.sqrt(m.velocity.x * m.velocity.x + m.velocity.y * m.velocity.y));
			}
			String info = "ID: " + entity.getId() + " (" + Math.round(t.pos.x) + "," + Math.round(t.pos.y) + ")" + vel;
			font.draw(batch, info, screenPos.x, screenPos.y);
			int nextLine = 20;
			for (int curComp = 0; curComp < components.size(); curComp++) {
				font.draw(batch, "[" + components.get(curComp).getClass().getSimpleName() + "]", screenPos.x, screenPos.y - nextLine * (curComp + 1));
			}
			
		}
		
	}

	/**  Draw position and speed of entity. */
	private void drawPos() {
		font.setColor(1, 1, 1, 1);
		for (Entity entity : objects) {
			TransformComponent t = transformMap.get(entity);
			MovementComponent m = movementMap.get(entity);
			
			Vector3 screenPos = RenderingSystem.getCam().project(t.pos.cpy());
			String vel = "";
			if (m != null) {
				vel = " ~ " + Math.round(Math.sqrt(m.velocity.x * m.velocity.x + m.velocity.y * m.velocity.y));
			}
			String info = Math.round(t.pos.x) + "," + Math.round(t.pos.y) + vel;
			font.draw(batch, info, screenPos.x, screenPos.y);			
			
		}
		
	}
	
	@Override 
	public void processEntity(Entity entity, float deltaTime) {
		objects.add(entity);
	}
	
}
