package com.spaceproject.systems;

import java.lang.reflect.Field;
import java.util.Set;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.BoundsComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;

public class DebugUISystem extends IteratingSystem implements Disposable {

	private Engine engine;
	
	//rendering
	private static OrthographicCamera cam;
	private static SpriteBatch batch;
	private static ShapeRenderer shape;
	private BitmapFont fontSmall, fontLarge;
	private Matrix4 projectionMatrix = new Matrix4();
	
	//textures
	private Texture texCompBack = TextureFactory.createTile(Color.GRAY);
	private Texture texCompSeparator = TextureFactory.createTile(Color.RED);
	
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
	private boolean drawMousePos = false;
	
	
	public DebugUISystem() {
		this(MyScreenAdapter.cam, MyScreenAdapter.batch, MyScreenAdapter.shape);
	}
	
	public DebugUISystem(OrthographicCamera camera, SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
		super(Family.all(TransformComponent.class).get());
		
		cam = camera;
		batch = spriteBatch;
		shape = shapeRenderer;
		fontSmall = FontFactory.createFont(FontFactory.fontBitstreamVM, 10);
		fontLarge = FontFactory.createFont(FontFactory.fontBitstreamVMBold, 20);
		objects = new Array<Entity>();
		
		
		boolean showInfo = false;
		if (showInfo) {
			System.out.println("\n------- sys info -------");
			System.getProperties().list(System.out);
			System.out.println("-------------------------\n");
			
			/*
			System.out.println(String.format("%s %s", Gdx.graphics.getPpiX(), Gdx.graphics.getPpiY()));
			for (DisplayMode mode : Gdx.graphics.getDisplayModes()) {
				System.out.println(String.format("%s %s %s %s", mode.width, mode.height, mode.bitsPerPixel, mode.refreshRate));
			}
			System.out.println("-------------------------\n");
			*/
		}
	}
	
	@Override
	public void addedToEngine(Engine engine) {		
		super.addedToEngine(engine);
		this.engine = engine;
	}

	@Override
	public void update(float delta) {
		//check key presses
		updateKeyToggles();
		
		//don't update if we aren't drawing
		if (!drawDebugUI) return;				
		super.update(delta);
		
		//set projection matrix so things render using correct coordinates
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());		
		batch.setProjectionMatrix(projectionMatrix);

		
		shape.begin(ShapeType.Line);
		
		//draw vector to visualize speed and direction
		if (drawVectors) drawMovementVectors();
		
		// draw ring to visualize orbit path
		if (drawOrbitPath) drawOrbitPath();
		
		//draw the bounding box (collision detection) for collidables
		if (drawBounds) drawBounds();
		
		if (drawMousePos) drawMouseLine();
		
		shape.end();
		
		
		batch.begin();
		
		//print debug menu
		if (drawMenu)  drawDebugMenu();
			
		//draw frames per second and entity count
		if (drawFPS) drawFPS();
		
		//draw entity position
		if (drawPos) drawPos();
		
		
		if (drawMousePos) drawMousePos();
		
		drawEntityList();
		
		//draw components on entity
		if (drawComponentList) drawComponentList();

		batch.end();	
		
		
		objects.clear();		
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

	public void drawMessage(String message, int x, int y) {
		batch.begin();
		fontSmall.draw(batch, message, x, y);
		batch.end();
	}

	/** Draw menu showing items to draw and toggle keys */
	private void drawDebugMenu() {
		fontSmall.setColor(1, 1, 1, 1);
		fontSmall.draw(batch, "***DEBUG [F3]***", 15, Gdx.graphics.getHeight() - 45);
		fontSmall.draw(batch, "[NUM0] Draw Pos: " + drawPos, 15, Gdx.graphics.getHeight() - 60);
		fontSmall.draw(batch, "[NUM1] Draw Component List: " + drawComponentList, 15, Gdx.graphics.getHeight() - 75);
		fontSmall.draw(batch, "[NUM2] Draw Bounds: " + drawBounds, 15, Gdx.graphics.getHeight() - 90);
		fontSmall.draw(batch, "[NUM3] Draw FPS: " + drawFPS, 15, Gdx.graphics.getHeight() - 105);
		fontSmall.draw(batch, "[NUM4] Draw Orbit Path: " + drawOrbitPath, 15, Gdx.graphics.getHeight() - 120);
		fontSmall.draw(batch, "[NUM5] Draw Vectors: " + drawVectors, 15, Gdx.graphics.getHeight() - 135);
		fontSmall.draw(batch, "[NUM9] Hide this menu.", 15, Gdx.graphics.getHeight() - 150);
	}

	/** Draw lines to represent speed and direction of entity */
	private void drawMovementVectors() {
		for (Entity entity : objects) {
			//get entities position and list of components
			TransformComponent t = Mappers.transform.get(entity);

			//calculate vector angle and length
			float scale = 4; //how long to make vectors (higher number is shorter line)
			float length = t.velocity.len();
			float angle = t.velocity.angle() * MathUtils.degreesToRadians;
			float pointX = t.pos.x + (length / scale * MathUtils.cos(angle));
			float pointY = t.pos.y + (length / scale * MathUtils.sin(angle));
			
			//draw line to represent movement
			shape.line(t.pos.x, t.pos.y, pointX, pointY, Color.RED, Color.MAGENTA);
		}
	}
	
	/** Draw orbit path, a ring to visualize objects orbit*/
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
	
	/** Draw bounding boxes (hitbox/collision detection) */
	private void drawBounds() {
		boolean polyTriangles = false;
		
		for (Entity entity : objects) { 
			BoundsComponent bounds = Mappers.bounds.get(entity);		
			TransformComponent t = Mappers.transform.get(entity);
			
			if (bounds != null) {
				//draw Axis-Aligned bounding box			
				Rectangle rect = bounds.poly.getBoundingRectangle();
				shape.setColor(1, 1, 0, 1);
				shape.rect(t.pos.x - rect.width/2, t.pos.y - rect.height/2, rect.width, rect.height);

				//draw Orientated bounding box
				shape.setColor(1, 0, 0, 1);
				shape.polygon(bounds.poly.getTransformedVertices());

				if (polyTriangles) {
					// draw triangles
					shape.setColor(Color.BLUE);
					FloatArray points = new FloatArray(bounds.poly.getTransformedVertices());
					ShortArray triangles = tri.computeTriangles(points, false);
					for (int i = 0; i < triangles.size; i += 3) {
						int p1 = triangles.get(i) * 2;
						int p2 = triangles.get(i + 1) * 2;
						int p3 = triangles.get(i + 2) * 2;
						shape.triangle(
								points.get(p1), points.get(p1 + 1), 
								points.get(p2), points.get(p2 + 1),
								points.get(p3), points.get(p3 + 1));
					}
				}
			}
		}
	}
	DelaunayTriangulator tri = new DelaunayTriangulator();

	/** Draw frames, entity count, position and memory info. */
	private void drawFPS() {
		//fps
		String frames = Integer.toString(Gdx.graphics.getFramesPerSecond());
		
		//threads
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		String threads = "  Threads: " + threadSet.size();
		
		//memory
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		String memory = "Mem: " + MyMath.formatBytes(used);
		
		//entity/component count
		int entityCount = engine.getEntities().size();
		int componentCount = 0;
		for (Entity ent : engine.getEntities()) {
			componentCount += ent.getComponents().size();
		}
		String count = "   E: " + entityCount + " - C: " + componentCount;
		
		//camera position
		String camera = String.format("Pos: %s %s  Zoom:%3$.2f", (int)cam.position.x, (int)cam.position.y, cam.zoom);
		
		//display info
		int x = 15;
		int y = Gdx.graphics.getHeight() - 15;
		fontLarge.setColor(1,1,1,1);
		fontLarge.draw(batch, frames + count, x, y);
		fontLarge.draw(batch, memory + threads, x, y - fontLarge.getLineHeight());
		fontLarge.draw(batch, camera, x, y - fontLarge.getLineHeight()*2);
	}
	
	private void drawEntityList() {
		float fontHeight = fontSmall.getLineHeight();
		int x = 30;
		int y = 30;
		fontSmall.setColor(1, 1, 1, 1);
		
		int i = 0;
		for (Entity entity : engine.getEntities()) {
			fontSmall.draw(batch, Integer.toHexString(entity.hashCode()), x, y+fontHeight*i++);
		}
	}
	
	/**  Draw all Entity components and fields. */
	private void drawComponentList() {
		float fontHeight = fontSmall.getLineHeight();
		int backWidth = 400;//width of background
		
		fontSmall.setColor(1, 1, 1, 1);
		
		for (Entity entity : objects) {
			//get entities position and list of components
			TransformComponent t = Mappers.transform.get(entity);			
			ImmutableArray<Component> components = entity.getComponents();
			
			//use Vector3.cpy() to project only the position and avoid modifying projection matrix for all coordinates
			Vector3 screenPos = cam.project(t.pos.cpy());
			
			//calculate spacing and offset for rendering
			int fields = 0;
			for (Component c : components) {
				fields += c.getClass().getFields().length;
			}
			float yOffset = fontHeight * fields/2;
			int curLine = 0;
			
			//draw all components/fields
			for (Component c : components) {
				//save component line to draw name
				float compLine = curLine;
				
				//draw all fields
				for (Field f : c.getClass().getFields()) {
					float yOffField = screenPos.y - (fontHeight * curLine) + yOffset;
					batch.draw(texCompBack, screenPos.x, yOffField, backWidth, -fontHeight);
					try {
						fontSmall.draw(batch, String.format("%-14s %s", f.getName(), f.get(c)), screenPos.x + 130, yOffField);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					curLine++;
				}
				
				//draw backing on empty components
				if (c.getClass().getFields().length == 0) {
					batch.draw(texCompBack, screenPos.x, screenPos.y - (fontHeight * curLine) + yOffset, backWidth, -fontHeight);
					curLine++;
				}
				
				//draw separating line
				batch.draw(texCompSeparator, screenPos.x, screenPos.y - (fontHeight * curLine) + yOffset, backWidth, 1);
				
				//draw component name
				float yOffComp = screenPos.y - (fontHeight * compLine) + yOffset;
				fontSmall.draw(batch, "[" + c.getClass().getSimpleName() + "]", screenPos.x, yOffComp);
			}
		}
		
	}

	/**  Draw position and speed of entity. */
	private void drawPos() {
		fontSmall.setColor(1, 1, 1, 1);
		for (Entity entity : objects) {
			TransformComponent t = Mappers.transform.get(entity);
			
			String vel = " ~ " + MyMath.round(t.velocity.len(), 1);
			String info = Math.round(t.pos.x) + "," + Math.round(t.pos.y) + vel;

			Vector3 screenPos = cam.project(t.pos.cpy());
			fontSmall.draw(batch, Integer.toHexString(entity.hashCode()) , screenPos.x, screenPos.y-15);
			fontSmall.draw(batch, info, screenPos.x, screenPos.y);
		}
	}
	
	private void drawMousePos() {
		int x = Gdx.input.getX();
		int y = Gdx.input.getY();
		
		Vector3 worldPos = cam.unproject(new Vector3(x,y,0));
		String localPos =  x + "," + y;
		fontSmall.draw(batch, localPos, x, Gdx.graphics.getHeight()-y);
		fontSmall.draw(batch, (int)worldPos.x + "," + (int)worldPos.y, x, Gdx.graphics.getHeight()-y+fontSmall.getLineHeight());
	}
	
	private void drawMouseLine() {
		int crossHairSize = 32;
		int x = Gdx.input.getX();
		int y = Gdx.input.getY();
		Vector3 worldPos = cam.unproject(new Vector3(x,y,0));
		shape.setColor(Color.BLACK);
		shape.line(worldPos.x, worldPos.y+crossHairSize, worldPos.x, worldPos.y-crossHairSize);
		shape.line(worldPos.x+crossHairSize, worldPos.y, worldPos.x-crossHairSize, worldPos.y);
	}
	
	
	@Override 
	public void processEntity(Entity entity, float deltaTime) {
		objects.add(entity);
	}

	
	@Override
	public void dispose() {
		texCompBack.dispose();
		texCompSeparator.dispose();
		fontSmall.dispose();
		fontLarge.dispose();
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
