package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
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

	private Engine engine;

	//rendering
	private static OrthographicCamera cam;
	private Matrix4 projectionMatrix = new Matrix4();	
	private ShapeRenderer shape = new ShapeRenderer();

	//entity storage
	private ImmutableArray<Entity> mapableObjects;
	private ImmutableArray<Entity> player;
	private ImmutableArray<Entity> killables;
	
	private boolean drawHud = true;
	private boolean drawEdgeMap = true;
	public static MapState drawMap = MapState.off;
	public enum MapState {
		full,
		mini,
		off
	}

	public static float spaceMapScale = 500;
	
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

	public static void CycleMapState() {
		switch (drawMap) {
			case full: drawMap = MapState.mini; break;
			case mini: drawMap = MapState.off; break;
			case off: drawMap = MapState.full; break;
		}
	}
	
	@Override
	public void update(float delta) {
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleHUD)) {
			drawHud = !drawHud;
			System.out.println("HUD: " + drawHud);
		}
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleEdgeMap)) {
			drawEdgeMap = !drawEdgeMap;
			System.out.println("Edge drawMap: " + drawEdgeMap);
		}
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleSpaceMap)) {
			//drawSpaceMap = !drawSpaceMap;
			CycleMapState();
			System.out.println("Space drawMap: " + drawMap);
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
		
		//if (drawSpaceMap)
		drawSpaceMap();
		
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
			Vector3 pos = cam.project(new Vector3(Mappers.transform.get(entity).pos.cpy(),0));
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
		int barWidth = 200;
		int barHeight = 12;
		int playerBarX = Gdx.graphics.getWidth()/2 - barWidth /2;
		int playerHPBarY = 55;
		int playerAmmoBarY = playerHPBarY - barHeight - 1;

		if (player == null || player.size() == 0) return;

		//draw health bar
		HealthComponent health = Mappers.health.get(player.first());		
		if (health != null) {
			float ratioHP = health.health / health.maxHealth;
			shape.setColor(barBackground);
			shape.rect(playerBarX, playerHPBarY, barWidth, barHeight);
			shape.setColor(new Color(1 - ratioHP, ratioHP, 0, opacity));
			shape.rect(playerBarX, playerHPBarY, barWidth * ratioHP, barHeight);
		}
		
		//draw ammo bar
		CannonComponent cannon = Mappers.cannon.get(player.first());
		if (cannon != null) {
			float ratioAmmo = (float) cannon.curAmmo / (float) cannon.maxAmmo;
			shape.setColor(barBackground);
			shape.rect(playerBarX, playerAmmoBarY, barWidth, barHeight);
			shape.setColor(Color.TEAL);
			shape.rect(playerBarX, playerAmmoBarY, barWidth * ratioAmmo, barHeight);


			for (int i = 0; i < cannon.maxAmmo; i++) {
				int x = playerBarX + (i * barWidth / cannon.maxAmmo);
				//draw recharge bar
				if (i == cannon.curAmmo) {
					shape.setColor(Color.SLATE);
					shape.rect(x, playerAmmoBarY, barWidth/cannon.maxAmmo*cannon.timerRechargeRate.ratio(), barHeight);
				}
				//draw divisions to mark individual ammo
				if (i > 0) {
					shape.setColor(Color.BLACK);
					shape.rectLine(x, playerAmmoBarY + barHeight, x, playerAmmoBarY,3);
				}
			}

			/*
			//draw recharge bar (style 2)
			if (cannon.curAmmo < cannon.maxAmmo) {
				int rechargeBarHeight = 2;
				shape.setColor(Color.SLATE);
				shape.rect(playerBarX, playerAmmoBarY, barWidth * cannon.timerRechargeRate.ratio(), rechargeBarHeight);
			}
			*/
		}

		/*
		//border
		shape.setColor(new Color(1,1,1,1));
		int thickness = 1;
		shape.rectLine(playerBarX, playerHPBarY+barHeight, playerBarX+barWidth, playerHPBarY+barHeight, thickness);//top
		shape.rectLine(playerBarX, playerHPBarY-barHeight, playerBarX+barWidth, playerHPBarY-barHeight,thickness);//bottom
		shape.rectLine(playerBarX, playerHPBarY+barHeight, playerBarX, playerHPBarY-barHeight, thickness);//left
		shape.rectLine(playerBarX+barWidth, playerHPBarY+barHeight, playerBarX+barWidth, playerHPBarY-barHeight, thickness);//right
		*/
	}

	/**
	 * Mark off-screen objects on edge of screen for navigation.
	 * TODO: load star drawMap markers based on point list instead of star entity for stars that aren't loaded yet
	 */
	private void drawEdgeMap() {
		//TODO: move these values into MapComponent or a config file
		float markerSmall = 3.5f; //min marker size
		float markerLarge = 8; //max marker size
		float distSmall = 8000; //distance when marker is small
		float distLarge = 2000; //distance when marker is large
		//gain and offset for transfer function: drawMap [3.5 - 8] to [8000 - 2000]
		double gain = (markerSmall-markerLarge)/(distSmall-distLarge);
		double offset = markerSmall - gain * distSmall;
		
		int padding = (int) (markerLarge + 4); //how close to draw from edge of screen (in pixels)
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();	
		int centerX = width/2;
		int centerY = height/2;
		int verticalEdge = (height - padding * 2) / 2;
		int horizontalEdge = (width - padding * 2) / 2;		

		boolean drawBorder = false;
		if (drawBorder) {
			shape.setColor(Color.BLACK);
			shape.line(padding, padding, padding, height-padding);//left
			shape.line(width - padding, padding, width - padding, height-padding);//right
			shape.line(padding, padding, width-padding, padding);//bottom
			shape.line(padding, height-padding, width - padding, height-padding);//top
		}

		for (Entity mapable : mapableObjects) {
			MapComponent map = Mappers.map.get(mapable);
			Vector3 screenPos = new Vector3(Mappers.transform.get(mapable).pos.cpy(),0);
			
			if (screenPos.dst(MyScreenAdapter.cam.position) > map.distance) {
				continue;
			}
			
			//set entity co'ords relative to center of screen
			screenPos.x -= MyScreenAdapter.cam.position.x;
			screenPos.y -= MyScreenAdapter.cam.position.y;
			
			//skip on screen entities
			//TODO: take camera zoom into account, calculate viewport coords
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
				markerX = -verticalEdge/slope;
				markerY = -verticalEdge;
			} else {
				//bottom
				markerX = verticalEdge/slope;
				markerY = verticalEdge;
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
			switch(drawMap.shape) {
				case circle: shape.circle(markerX, markerY, (float)size);
				case square: shape.rect
				case triangle: shape.poly
			}
			 */
		}

	}



	private void drawSpaceMap() {
		if (spaceMapScale < 1) spaceMapScale = 1;

		if (drawMap == MapState.off)
			return;

		//TODO: fix map
		//[...] fix minimap
		//[...] simplify grid calculation
		//[ ] make drawMap item relative to middle of drawMap instead of middle of screen(Gdx.graphics.getWidth()/2), verify with small non-centered drawMap
		//[ ] draw grid pos

		int chunkSize = (int) Math.pow(2, 17 - 1);
		int borderWidth = 3;
		int size = 6;


		Rectangle mapBacking;
		if (drawMap == MapState.full) {
			int edgePad = 50;
			mapBacking = new Rectangle(edgePad, edgePad, Gdx.graphics.getWidth() - edgePad * 2, Gdx.graphics.getHeight() - edgePad * 2);
		} else {
			int miniWidth = 320;
			int miniHeight = 240;
			/*
			//top-right
			mapBacking = new Rectangle(
					Gdx.graphics.getWidth() - miniWidth-10,
					Gdx.graphics.getHeight() - miniHeight-10,
					miniWidth,
					miniHeight); */

			//bottom-left
			mapBacking = new Rectangle(
					Gdx.graphics.getWidth() - miniWidth-10,
					10,
					miniWidth,
					miniHeight);
			/*
			//bottom-right
			mapBacking = new Rectangle(
					10,
					10,
					miniWidth,
					miniHeight); */
		}



		float centerX = mapBacking.x + mapBacking.width/2;
		float centerY = mapBacking.y + mapBacking.height/2;
		float offX = (mapBacking.x + mapBacking.width/2);
		float offY = (mapBacking.y + mapBacking.height/2);

		//draw backing
		shape.setColor(0, 0, 0, 0.8f);
		shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, mapBacking.height);


		//draw mouse pos
		shape.setColor(1f,0.2f,0.2f,1f);
		int mX = Gdx.input.getX();
		int mY = Gdx.graphics.getHeight()-Gdx.input.getY();
		if (mX > mapBacking.x && mX < mapBacking.x+mapBacking.width && mY > mapBacking.y && mY < mapBacking.y+mapBacking.height) {
			shape.line(mapBacking.y, Gdx.graphics.getHeight()-Gdx.input.getY(), mapBacking.y+mapBacking.width, Gdx.graphics.getHeight()-Gdx.input.getY());//horizontal
			shape.line(Gdx.input.getX(), mapBacking.x, Gdx.input.getX(), mapBacking.x+mapBacking.height);//vertical
		}


		//draw grid X
		shape.setColor(0.2f,0.2f,0.2f,0.8f);
		int startX = (int)(((mapBacking.x-Gdx.graphics.getWidth()/2)*spaceMapScale)+MyScreenAdapter.cam.position.x)/chunkSize;
		int endX = (int)(((mapBacking.x+mapBacking.width-Gdx.graphics.getWidth()/2)*spaceMapScale)+MyScreenAdapter.cam.position.x)/chunkSize;
		for (int i = startX; i < endX+1; i++) {
			float finalX = ((i*chunkSize)-MyScreenAdapter.cam.position.x )/ spaceMapScale +Gdx.graphics.getWidth()/2;
			shape.rect(finalX, mapBacking.y, 1, mapBacking.height);
		}
		//draw grid Y
		int startY = (int)(((mapBacking.y-Gdx.graphics.getHeight()/2)*spaceMapScale)+MyScreenAdapter.cam.position.y)/chunkSize;
		int endY = (int)(((mapBacking.y+mapBacking.height-Gdx.graphics.getHeight()/2)*spaceMapScale)+MyScreenAdapter.cam.position.y)/chunkSize;
		for (int i = startY; i < endY+1; i++) {
			float finalY = ((i*chunkSize)-MyScreenAdapter.cam.position.y )/ spaceMapScale +Gdx.graphics.getHeight()/2;
			shape.rect(mapBacking.x, finalY, mapBacking.width, 1);
		}


		//draw drawMap objects
		shape.setColor(1, 1, 0, 1);
		SpaceLoadingSystem spaceLoader = engine.getSystem(SpaceLoadingSystem.class);
		if (spaceLoader != null) {
			for (Vector2 p : spaceLoader.getPoints()) {
				// n = relative pos / scale + mapPos
				float x = ((p.x - MyScreenAdapter.cam.position.x) / spaceMapScale) + (mapBacking.x + mapBacking.width / 2);//+Gdx.graphics.getWidth()/2;
				float y = ((p.y - MyScreenAdapter.cam.position.y) / spaceMapScale) + (mapBacking.y + mapBacking.height / 2);//+Gdx.graphics.getHeight()/2;


				if (mapBacking.contains(x, y)) shape.circle(x, y, size);

			}
		}


		//draw border
		shape.setColor(0.6f,0.6f,0.6f,1f);
		shape.rect(mapBacking.x, mapBacking.height+mapBacking.y-borderWidth, mapBacking.width, borderWidth);//top
		shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, borderWidth);//bottom
		shape.rect(mapBacking.x, mapBacking.y, borderWidth, mapBacking.height);//left
		shape.rect(mapBacking.width+mapBacking.x-borderWidth, mapBacking.y, borderWidth, mapBacking.height);//right
	}
	
}
