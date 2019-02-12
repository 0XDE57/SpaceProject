package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.MapState;
import com.spaceproject.ui.Menu;
import com.spaceproject.ui.MiniMap;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;


public class HUDSystem extends EntitySystem implements RequireGameContext {

	private Stage stage;
	private Menu menu;

	//rendering
	private OrthographicCamera cam;
	private Matrix4 projectionMatrix = new Matrix4();	
	private ShapeRenderer shape;
	private SpriteBatch batch;


	//entity storage
	private ImmutableArray<Entity> mapableObjects;
	private ImmutableArray<Entity> player;
	private ImmutableArray<Entity> killables;

	private MiniMap miniMap = new MiniMap();
	
	private boolean drawHud = true;
	private boolean drawEdgeMap = true;
	
	
	public HUDSystem() {
		cam = MyScreenAdapter.cam;
		shape = MyScreenAdapter.shape;
		batch = MyScreenAdapter.batch;
		
		stage = new Stage(new ScreenViewport());

	}
	
	@Override
	public void initContext(GameScreen gameScreen) {
		gameScreen.getInputMultiplexer().addProcessor(0, getStage());
	}
	
	
	@Override
	public void addedToEngine(Engine engine) {
		mapableObjects = engine.getEntitiesFor(Family.all(MapComponent.class, TransformComponent.class).get());
		player = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
		killables = engine.getEntitiesFor(Family.all(HealthComponent.class, TransformComponent.class).exclude(CameraFocusComponent.class).get());


		menu = new Menu(false, engine);
		//stage.addListener((InputListener)this);
		
		stage.addListener(new InputListener() {
			@Override
			public boolean keyDown (InputEvent event, int keycode) {
				if (menu.switchTabForKey(keycode)) {
					if (!menu.isVisible()) {
						menu.show(stage);
					}
					return true;
				}
				return menu.isVisible();
			}

			@Override
			public boolean keyUp(InputEvent event, int keycode) {
				super.keyUp(event, keycode);
				return menu.isVisible();
			}

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				super.touchDown(event, x, y, pointer, button);
				return menu.isVisible();
			}
		});
	}


	@Override
	public void update(float delta) {

		CheckInput();

		drawHUD();

		//draw menu
		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		stage.draw();


		Entity p = player.size() > 0 ? player.first() : null;
		miniMap.drawSpaceMap(getEngine(), shape, batch, p, mapableObjects);
	}

	private void drawHUD() {
		if (!drawHud) return;
		//set projection matrix so things render using correct coordinates
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		shape.setProjectionMatrix(projectionMatrix);
		batch.setProjectionMatrix(projectionMatrix);

		//enable transparency
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		shape.begin(ShapeType.Filled);

		drawPlayerStatus();

		if (drawEdgeMap) drawEdgeMap();

		drawHealthBars();

		shape.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);


		//TODO: temporary fix. engine system priority....
		MobileInputSystem mobileUI = getEngine().getSystem(MobileInputSystem.class);
		if (mobileUI != null)
			mobileUI.drawControls();
	}

	private void CheckInput() {
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleHUD)) {
			drawHud = !drawHud;
            Gdx.app.log(this.getClass().getSimpleName(),"HUD: " + drawHud);
		}
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleEdgeMap)) {
			drawEdgeMap = !drawEdgeMap;
            Gdx.app.log(this.getClass().getSimpleName(),"Edge mapState: " + drawEdgeMap);
		}
		if (Gdx.input.isKeyJustPressed(SpaceProject.keycfg.toggleSpaceMap)) {
			miniMap.cycleMapState();
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
			miniMap.cycleMiniMapPosition();
		}
		if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
			if (miniMap.mapState != MapState.off) {
				miniMap.resetMapScale();
			}
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
			stage.setDebugAll(!stage.isDebugAll());
		}
	}


	/**
	 * Draw health bars on entities.
	 */
	private void drawHealthBars() {
		//bar dimensions
		int barLength = SpaceProject.uicfg.entityHPbarLength;
		int barWidth = SpaceProject.uicfg.entityHPbarWidth;
		int yOffset = SpaceProject.uicfg.entityHPbarYOffset;

		for (Entity entity : killables) {
			Vector3 pos = cam.project(new Vector3(Mappers.transform.get(entity).pos.cpy(),0));
			HealthComponent health = Mappers.health.get(entity);
			
			
			//ignore full health
			if (!SpaceProject.uicfg.renderFullHealth && health.health == health.maxHealth) {
				continue;
			}
			
			//background
			shape.setColor(SpaceProject.uicfg.entityHPbarBackground);
			shape.rect(pos.x-barLength/2, pos.y+yOffset, barLength, barWidth);
			
			//health
			float ratio = health.health/health.maxHealth;
			shape.setColor(1 - ratio, ratio, 0, SpaceProject.uicfg.entityHPbarOpacity); //creates color between red and green
			shape.rect(pos.x-barLength/2, pos.y+yOffset, barLength * ratio, barWidth);
		}
			
	}

	/**
	 * Draw the players health and ammo bar.
	 */
	private void drawPlayerStatus() {
		int barWidth = SpaceProject.uicfg.playerHPBarWidth;
		int barHeight = SpaceProject.uicfg.playerHPBarHeight;
		int playerBarX = Gdx.graphics.getWidth()/2 - barWidth /2;
		int playerHPBarY = SpaceProject.uicfg.playerHPBarY;
		int playerAmmoBarY = playerHPBarY - barHeight - 1;

		if (player == null || player.size() == 0) return;

		//draw health bar
		HealthComponent health = Mappers.health.get(player.first());		
		if (health != null) {
			float ratioHP = health.health / health.maxHealth;
			shape.setColor(SpaceProject.uicfg.entityHPbarBackground);
			shape.rect(playerBarX, playerHPBarY, barWidth, barHeight);
			shape.setColor(1 - ratioHP, ratioHP, 0, SpaceProject.uicfg.entityHPbarOpacity);
			shape.rect(playerBarX, playerHPBarY, barWidth * ratioHP, barHeight);
		}

		ShieldComponent shield = Mappers.shield.get(player.first());
		if (shield != null) {
			float ratioShield = shield.radius / shield.maxRadius;
			if (shield.active) {
				shape.setColor(shield.color);
			} else {
				shape.setColor(shield.color.r, shield.color.g, shield.color.b, 0.25f);
			}
			shape.rect(playerBarX, playerHPBarY, barWidth * ratioShield, barHeight);
		}
		
		//draw ammo bar
		CannonComponent cannon = Mappers.cannon.get(player.first());
		if (cannon != null) {
			float ratioAmmo = (float) cannon.curAmmo / (float) cannon.maxAmmo;
			shape.setColor(SpaceProject.uicfg.entityHPbarBackground);
			shape.rect(playerBarX, playerAmmoBarY, barWidth, barHeight);
			shape.setColor(SpaceProject.uicfg.playerAmmoBarColor);
			shape.rect(playerBarX, playerAmmoBarY, barWidth * ratioAmmo, barHeight);


			for (int i = 0; i < cannon.maxAmmo; i++) {
				int x = playerBarX + (i * barWidth / cannon.maxAmmo);
				//draw recharge bar
				if (i == cannon.curAmmo) {
					shape.setColor(SpaceProject.uicfg.playerAmmoBarRechargeColor);
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
	 * TODO: load star mapState markers based on point list instead of star entity for stars that aren't loaded yet
	 */
	private void drawEdgeMap() {
		//TODO: move these values into MapComponent or a config file
		float markerSmall = 3.5f; //min marker size
		float markerLarge = 8; //max marker size
		float distSmall = 8000; //distance when marker is small
		float distLarge = 2000; //distance when marker is large
		//gain and offset for transfer function: mapState [3.5 - 8] to [8000 - 2000]
		double gain = (markerSmall-markerLarge)/(distSmall-distLarge);
		double offset = markerSmall - gain * distSmall;
		
		int padding = (int)(markerLarge + 4); //how close to draw from edge of screen (in pixels)
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
			float markerX, markerY;
			
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
			switch(mapState.shape) {
				case circle: shape.circle(markerX, markerY, (float)size);
				case square: shape.rect
				case triangle: shape.poly
			}
			 */
		}

	}


	public MiniMap getMiniMap() {
		return miniMap;
	}


	public Stage getStage() {
		return stage;
	}

	public Menu getMenu() {
		return menu;
	}
	
	public void resize(int width, int height) {
		menu.setSize(Gdx.graphics.getWidth()-150, Gdx.graphics.getHeight()-150);
		stage.getViewport().update(width, height, true);
		
		miniMap.updateMapPosition();
	}
}
