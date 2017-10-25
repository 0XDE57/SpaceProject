package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.MyScreenAdapter;

public class HUDSystem extends EntitySystem {
	
	//rendering
	private static OrthographicCamera cam;
	private Matrix4 projectionMatrix = new Matrix4();	
	private ShapeRenderer shape = new ShapeRenderer();
	
	private Engine engine;
	
	//entity storage
	private ImmutableArray<Entity> mapableObjects;
	private ImmutableArray<Entity> player;
	private ImmutableArray<Entity> killables;
	
	private boolean drawHud = true;
	private boolean drawEdgeMap = true;
	public static boolean drawSpaceMap = false;
	
	float opacity = 0.7f;
	Color barBackground = new Color(1,1,1,0.5f);
	
	public HUDSystem() {
		this(MyScreenAdapter.cam);
	}
	
	public HUDSystem(OrthographicCamera camera) {
		cam = camera;
	}

	@Override
	public void addedToEngine(Engine engine) {
		this.engine = engine;
		mapableObjects = engine.getEntitiesFor(Family.all(MapComponent.class, TransformComponent.class).get());
		player = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
		killables = engine.getEntitiesFor(Family.all(HealthComponent.class, TransformComponent.class).exclude(CameraFocusComponent.class).get());
	}
	
	@Override
	public void update(float delta) {
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleHUD)) {
			drawHud = !drawHud;
			System.out.println("HUD: " + drawHud);
		}
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleEdgeMap)) {
			drawEdgeMap = !drawEdgeMap;
			System.out.println("Edge map: " + drawEdgeMap);
		}
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleSpaceMap)) {
			drawSpaceMap = !drawSpaceMap;
			System.out.println("Space map: " + drawSpaceMap);
		}
		
		if (!drawHud) return;
		
		//set projection matrix so things render using correct coordinates
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); 
		shape.setProjectionMatrix(projectionMatrix);
		
		//enable transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		shape.begin(ShapeType.Filled);
		
		drawPlayerStatus();
		
		if (drawEdgeMap) drawEdgeMap();
		
		if (drawSpaceMap) drawSpaceMap();
		
		drawHealthBars();
		
		shape.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);
		
		
		//TODO: temporary fix. engine system priority....
		MobileInputSystem mobileUI = engine.getSystem(MobileInputSystem.class);
		if (mobileUI != null)
			mobileUI.drawControls();
		
	}

	/**
	 * Draw health bars on entities.
	 */
	private void drawHealthBars() {		
		//bar dimensions
		int barLength = 40;
		int barWidth = 8;
		int yOffset = -20; //position from entity		
		
		for (Entity entity : killables) {
			Vector3 pos = cam.project(Mappers.transform.get(entity).pos.cpy());
			HealthComponent health = Mappers.health.get(entity);
			
			
			//ignore full health
			if (health.health == health.maxHealth) {
				//continue;
			}
			
			//background
			shape.setColor(barBackground);
			shape.rect(pos.x-barLength/2, pos.y+yOffset, barLength, barWidth);
			
			//health
			float ratio = health.health/health.maxHealth;
			shape.setColor(new Color(1 - ratio, ratio, 0, opacity)); //creates color between red and green
			shape.rect(pos.x-barLength/2, pos.y+yOffset, barLength * ratio, barWidth);
		}
			
	}

	/**
	 * Draw the players health and ammo bar.
	 */
	private void drawPlayerStatus() {
		int barLength = 200;
		int barWidth = 12;
		int playerBarX = Gdx.graphics.getWidth()/2 - barLength/2;
		int playerHPBarY = 55;
		int playerAmmoBarY = playerHPBarY - barWidth - 1;

		//TODO add backing/border. Maybe caps at ends of bar. Make look nice.
		if (player == null || player.size() == 0) return;
		HealthComponent health = Mappers.health.get(player.first());		
		if (health == null) return;
		
		//draw health bar
		float ratioHP = health.health/health.maxHealth;
		shape.setColor(barBackground);
		shape.rect(playerBarX, playerHPBarY, barLength, barWidth);
		shape.setColor(new Color(1 - ratioHP, ratioHP, 0, opacity));
		shape.rect(playerBarX, playerHPBarY, barLength * ratioHP, barWidth);
				
		
		//draw ammo
		CannonComponent cannon = Mappers.cannon.get(player.first());
		if (cannon == null) return;
		
		
		//draw ammo bar
		float ratioAmmo = (float) cannon.curAmmo / (float) cannon.maxAmmo;
		shape.setColor(barBackground);
		shape.rect(playerBarX, playerAmmoBarY, barLength, barWidth);
		shape.setColor(Color.CYAN);
		shape.rect(playerBarX, playerAmmoBarY, barLength * ratioAmmo, barWidth);
		
		//draw divisions to mark individual ammo
		shape.setColor(Color.BLACK);
		for (int i = 1; i < cannon.maxAmmo; i++) {
			int x = playerBarX + (i * barLength / cannon.maxAmmo);
			shape.line(x, playerAmmoBarY + barWidth, x, playerAmmoBarY);
			shape.line(x + 1, playerAmmoBarY + barWidth, x + 1, playerAmmoBarY);
			shape.line(x - 1, playerAmmoBarY + barWidth, x - 1, playerAmmoBarY);
		}
		
				
		//OLD bar. Length of bar depends on ammo capacity. Looks bad with very large or small capacities. 
		/*
		Color bar = new Color(1, 1, 1, 0.4f);
		Color on = new Color(0.15f, 0.5f, 0.9f, 0.9f);
		Color off = new Color(0f, 0f, 0f, 0.6f);
				
		int posY = 30; //pixels from bottom off screen
		int posX = Gdx.graphics.getWidth() / 2;
		int border = 5; //width of border on background bar
		int padding = 4; //space between indicators
		int indicatorSize = 15;
		int barWidth1 = cannon.maxAmmo * (indicatorSize + (padding * 2));
		
		//draw bar
		shape.setColor(bar);		
		shape.rect(posX-barWidth1/2+padding-border, posY-border, posX, (barWidth1/2) + (border*2), (barWidth1-padding*2) + (border*2), indicatorSize + (border*2), 1, 1, 0);
		
		//draw indicators
		for (int i = 0; i < cannon.maxAmmo; ++i) {			
			//Z = A * (B + (C * 2)) + X - ((D * (B + C * 2))/2) + C
			//TODO: It works, but can this be simplified?
			shape.setColor(cannon.curAmmo <= i ? off : on);
			shape.rect((i * (indicatorSize + (padding * 2))) + posX - (barWidth1/2) + padding, posY, indicatorSize/2, indicatorSize/2, indicatorSize, indicatorSize, 1, 1, 0);
		}*/
		
	}

	/**
	 * Mark off-screen objects on edge of screen for navigation.
	 * TODO: load star map markers based on point list instead of star entity for stars that aren't loaded yet
	 */
	private void drawEdgeMap() {
		//TODO: move these values into MapComponent
		float markerSmall = 3.5f; //min marker size
		float markerLarge = 8; //max marker size
		float distSmall = 8000; //distance when marker is small
		float distLarge = 2000; //distance when marker is large
		//gain and offset for transfer function: map [3.5 - 8] to [8000 - 2000]
		double gain = (markerSmall-markerLarge)/(distSmall-distLarge);
		double offset = markerSmall - gain * distSmall;
		
		int padding = (int) (markerLarge + 4); //how close to draw from edge of screen (in pixels)
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();	
		int centerX = width/2;
		int centerY = height/2;
		int verticleEdge = (height - padding * 2) / 2;
		int horizontalEdge = (width - padding * 2) / 2;		
		
		for (Entity mapable : mapableObjects) {
			MapComponent map = Mappers.map.get(mapable);
			Vector3 screenPos = Mappers.transform.get(mapable).pos.cpy();
			
			if (screenPos.dst(MyScreenAdapter.cam.position) > map.distance) {
				continue;
			}
			
			//set entity co'ords relative to center of screen
			screenPos.x -= MyScreenAdapter.cam.position.x;
			screenPos.y -= MyScreenAdapter.cam.position.y;
			
			//skip on screen entities
			int z = 100; //how close to edge of screen to ignore
			if (screenPos.x + z > -centerX && screenPos.x - z < centerX 
					&& screenPos.y + z > -centerY && screenPos.y - z < centerY) {			
				continue;
			}
			
			//position to draw marker
			float markerX = 0, markerY = 0; 
			
			//calculate slope of line (y = mx+b)
			float slope = screenPos.y / screenPos.x;
			
			//calculate where to position the marker
			if (screenPos.y < 0) {
				//top
				markerX = -verticleEdge/slope;
				markerY = -verticleEdge;
			} else {
				//bottom
				markerX = verticleEdge/slope;
				markerY = verticleEdge;
			}
			
			if (markerX < -horizontalEdge) {
				//left
				markerX = -horizontalEdge;
				markerY = slope * -horizontalEdge;
			} else if (markerX > horizontalEdge) {
				//right
				markerX = horizontalEdge;
				markerY = slope * horizontalEdge;
			}
			
			//set co'ords relative to center screen
			markerX += centerX;
			markerY += centerY;
			
			//calculate size of marker based on distance
			float dist = MyMath.distance(screenPos.x, screenPos.y, centerX, centerY);
			double size = gain * dist + offset;
			if (size < markerSmall) size = markerSmall;
			if (size > markerLarge) size = markerLarge;
			
			//draw marker
			shape.setColor(map.color);
			shape.circle(markerX, markerY, (float)size);
			
			/*
			switch(map.shape) {
				case circle: shape.circle(markerX, markerY, (float)size);
				case square: shape.rect
				case triangle: shape.poly
			}
			 */
		}
		
		/*
		//I'm not sure how to make this look pretty or if borders should be added...
		//Maybe research some UI design.
		
		//draw borders
		Color outer = new Color(0.6f, 1, 0.7f, 0.3f);
		Color inner = new Color(1, 1, 1, 0.2f);
		//left
		shape.rect(0, 0, padding*2, height, outer, inner, inner, outer);
		shape.line(padding*2, 0, padding*2, height);
		//right
		shape.rect(width - padding*2, 0, padding*2, height, inner, outer, outer, inner);
		//bottom
		shape.rect(0, 0, width, padding*2, outer, outer, inner, inner);
		//top
		shape.rect(0, height - padding*2, width, padding*2, inner, inner, outer, outer);
		*/
	}
	
	private float scale = 500;
	private void drawSpaceMap() {
		if (Gdx.input.isKeyPressed(Keys.I)) scale++;
		if (Gdx.input.isKeyPressed(Keys.O)) scale--;
		if (scale < 1) scale = 1;
		int edgePad = 30;
		int size = 5;
		Rectangle r = new Rectangle(edgePad, edgePad, Gdx.graphics.getWidth() - edgePad*2, Gdx.graphics.getHeight() - edgePad*2);
		
		shape.setColor(0, 1, 1, 0.5f);
		shape.rect(r.x, r.y, r.width, r.height);
		
		shape.setColor(1, 1, 0, 1);
		for (Vector2 p : engine.getSystem(SpaceLoadingSystem.class).getPoints()) {
			float x = ((p.x-MyScreenAdapter.cam.position.x)/scale)+Gdx.graphics.getWidth()/2;
			float y = ((p.y-MyScreenAdapter.cam.position.y)/scale)+Gdx.graphics.getHeight()/2;
			
			
			if (r.contains(x, y)) shape.circle(x, y, size);

		}
		//screenPos.x -= MyScreenAdapter.cam.position.x;
		//screenPos.y -= MyScreenAdapter.cam.position.y;
		//Vector2 t = new Vector2(700,700);
		//shape.circle((t.x-MyScreenAdapter.cam.position.x)+Gdx.graphics.getWidth()/2, (t.x-MyScreenAdapter.cam.position.y)+Gdx.graphics.getHeight()/2, 10);
	}
	
}
