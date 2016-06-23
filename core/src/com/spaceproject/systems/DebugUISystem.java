package com.spaceproject.systems;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.MovementComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyIteratingSystem;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;

public class DebugUISystem extends MyIteratingSystem implements Disposable {

	//rendering
	private static OrthographicCamera cam;
	private static SpriteBatch batch;
	private static ShapeRenderer shape;
	private BitmapFont font;
	private Matrix4 projectionMatrix = new Matrix4();
	
	//entity storage
	private Array<Entity> objects;
	
	//config
	private boolean drawDebugUI = true;
	private boolean drawMenu = false;
	private boolean drawFPS = true;
	private boolean drawComponentList = false;
	private boolean drawPos = false;
	private boolean drawBounds = false;
	private boolean drawOrbitPath = false;
	private boolean drawVectors = false;
	
	//entity and component counting
	private float countTimer = 50;
	private float curCountTime = countTimer;
	private int entityCount = 0;
	private int componentCount = 0;
	
	public DebugUISystem() {
		this(MyScreenAdapter.cam, MyScreenAdapter.batch, MyScreenAdapter.shape);
	}
	
	public DebugUISystem(OrthographicCamera camera, SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
		super(Family.all(TransformComponent.class).get());
		
		cam = camera;
		batch = spriteBatch;
		shape = shapeRenderer;
		font = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 15);
		
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
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());		
		batch.setProjectionMatrix(projectionMatrix);		
		
		//enable blending for transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		
		//draw filled shapes
		shape.begin(ShapeType.Filled);
			
		//draw light background for text visibility
		if (drawComponentList) drawComponentListBack();
			
		shape.end();
		Gdx.gl.glDisable(GL20.GL_BLEND); //disable blending
				
		
		//draw non-filled shapes
		shape.begin(ShapeType.Line);
		
		//draw vector to visualize speed and direction
		if (drawVectors) drawMovementVectors();
		
		// draw ring to visualize orbit path
		if (drawOrbitPath) drawOrbitPath();
		
		//draw the bounding box (collision detection) for collidables
		if (drawBounds) drawBounds();
		
		shape.end();
		
		
		
		//draw batch
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
			TransformComponent t = Mappers.transform.get(entity);
			MovementComponent m = Mappers.movement.get(entity);
			if (m == null) continue;

			//calculate vector angle and length
			float scale = 4; //how long to make vectors (higher number is shorter line)
			float length = m.velocity.len();
			float angle = m.velocity.angle() * MathUtils.degreesToRadians;
			float pointX = t.pos.x + (length / scale * MathUtils.cos(angle));
			float pointY = t.pos.y + (length / scale * MathUtils.sin(angle));
			
			//draw line to represent movement
			shape.line(t.pos.x, t.pos.y, pointX, pointY, Color.RED, Color.MAGENTA);
		}
	}

	private void updateKeyToggles() {
		//toggle debug
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleDebug)) {
			drawDebugUI = !drawDebugUI;
			System.out.println("DEBUG UI: " + drawDebugUI);
		}
		
		//toggle pos
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.togglePos)) {
			drawPos = !drawPos;
			if(drawComponentList) {
				drawComponentList = false;
			}
			System.out.println("[debug] draw pos: " + drawPos);
		}
		
		//toggle components
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleComponents)) {
			drawComponentList = !drawComponentList;
			if (drawPos) {
				drawPos = false;
			}
			System.out.println("[debug] draw component list: " + drawComponentList);
		}
		
		//toggle bounds		
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleBounds)) {
			drawBounds = !drawBounds;
			System.out.println("[debug] draw bounds: " + drawBounds);
		}
		
		//toggle fps
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleFPS)) {
			drawFPS = !drawFPS;
			System.out.println("[debug] draw FPS: " + drawFPS);
		}
			
		//toggle orbit circle
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleOrbit)) {
			drawOrbitPath = !drawOrbitPath;
			System.out.println("[debug] draw orbit path: " + drawOrbitPath);
		}
		
		
		//toggle vector
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleVector)) {
			drawVectors = !drawVectors;
			System.out.println("[debug] draw vectors: " + drawVectors);
		}
		
		//toggle menu
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleMenu)) {
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
		font.draw(batch, "[NUM5] Draw Vectors: " + drawVectors, 15, Gdx.graphics.getHeight() - 135);
		font.draw(batch, "[NUM9] Hide this menu.", 15, Gdx.graphics.getHeight() - 150);
	}

	/** draw orbit path, a ring to visualize objects orbit*/
	private void drawOrbitPath() {
		shape.setColor(1f, 1f, 1, 1);
		for (Entity entity : objects) {
			OrbitComponent orbit = Mappers.orbit.get(entity);
			if (orbit != null && orbit.parent != null) {
				TransformComponent parentPos = Mappers.transform.get(orbit.parent);
				TransformComponent entityPos = Mappers.transform.get(entity);
				float distance = MyMath.distance(parentPos.pos.x, parentPos.pos.y, entityPos.pos.x, entityPos.pos.y);
				shape.circle(parentPos.pos.x, parentPos.pos.y, distance);
				shape.line(parentPos.pos.x, parentPos.pos.y, entityPos.pos.x, entityPos.pos.y);
			}
		}
	}
	
	/** draw bounding boxes (hitbox/collision detection) */
	private void drawBounds() {
		for (Entity entity : objects) { 
			BoundsComponent bounds = Mappers.bounds.get(entity);		
			TransformComponent t = Mappers.transform.get(entity);
			
			if (bounds != null) {
				//draw Axis-Aligned bounding box			
				Rectangle rect = bounds.poly.getBoundingRectangle();
				shape.setColor(1, 1, 0, 1);
				shape.rect(t.pos.x - rect.width/2, t.pos.y - rect.height/2, rect.width, rect.height);

				//draw Orientated bounding box
				bounds.poly.setPosition(bounds.poly.getX(), bounds.poly.getY());
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
			TransformComponent transform = Mappers.transform.get(entity);
			//draw rectangle with size relative to number of components and text size (20). 
			//210 box width - magic number assuming no component name will be that long 
			shape.rect(transform.pos.x-padding, transform.pos.y+padding, 210, ((-entity.getComponents().size() - 1) * 20) - padding);
		}
	}

	/**  Draw Entity ID, position and list of components attached. */
	private void drawComponentList() {
		font.setColor(1, 1, 1, 1);
		for (Entity entity : objects) {
			//get entities position and list of components
			TransformComponent t = Mappers.transform.get(entity);			
			ImmutableArray<Component> components = entity.getComponents();
			
			//use Vector3.cpy() to project only the position and avoid modifying projection matrix for all coordinates
			Vector3 screenPos = cam.project(t.pos.cpy());
					
			//if has movement
			MovementComponent m = Mappers.movement.get(entity);
			String vel = "";
			if (m != null) {
				vel = " ~ " + MyMath.round(m.velocity.len(), 1);
			}
			
			//if has id
			VehicleComponent v = Mappers.vehicle.get(entity);
			String id = "";
			if (v != null) {
				id = "ID: " + v.id + " ";
			}
			
			//print current ID and position in world and a list of all components
			String info = id + "(" + MyMath.round(t.pos.x, 1) + "," + MyMath.round(t.pos.y, 1) + ")" + vel;
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
			TransformComponent t = Mappers.transform.get(entity);
			MovementComponent m = Mappers.movement.get(entity);
			
			Vector3 screenPos = cam.project(t.pos.cpy());
			String vel = "";
			if (m != null) {
				vel = " ~ " + MyMath.round(m.velocity.len(), 1);
			}
			String info = Math.round(t.pos.x) + "," + Math.round(t.pos.y) + vel;
			font.draw(batch, info, screenPos.x, screenPos.y);			
			
		}
		
	}
	
	@Override 
	public void processEntity(Entity entity, float deltaTime) {
		objects.add(entity);
	}

	
	@Override
	public void dispose() {
		
		//font.dispose();
		//batch.dispose();
		//shape.dispose(); //crashes: 
		/*
		EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x0000000054554370, pid=5604, tid=2364
		Problematic frame:
	 	C  [atio6axx.dll+0x3c4370]
		 */
	}
	
}
