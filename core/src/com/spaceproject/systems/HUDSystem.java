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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.map.MapState;
import com.spaceproject.ui.menu.Menu;
import com.spaceproject.ui.map.MiniMap;
import com.spaceproject.ui.ScreenTransitionOverlay;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.IScreenResizeListener;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;


public class HUDSystem extends EntitySystem implements IRequireGameContext, IScreenResizeListener {
    
    private Stage stage;
    private Menu menu;
    
    //rendering
    private OrthographicCamera cam;
    private Matrix4 projectionMatrix = new Matrix4();
    private ShapeRenderer shape;
    private SpriteBatch batch;
    
    
    //entity storage
    private ImmutableArray<Entity> mapableEntities;
    private ImmutableArray<Entity> player;
    private ImmutableArray<Entity> killableEntities;
    
    private MiniMap miniMap;
    
    private boolean drawHud = true;
    private boolean drawEdgeMap = true;
    
    private ScreenTransitionOverlay screenTransitionOverlay;
    
    public HUDSystem() {
        cam = MyScreenAdapter.cam;
        shape = MyScreenAdapter.shape;
        batch = MyScreenAdapter.batch;
        stage = new Stage(new ScreenViewport());
        
        miniMap = new MiniMap(SpaceProject.miniMapCFG);
        screenTransitionOverlay = new ScreenTransitionOverlay();
    }
    
    @Override
    public void initContext(GameScreen gameScreen) {
        gameScreen.getInputMultiplexer().addProcessor(0, getStage());
    }
    
    
    @Override
    public void addedToEngine(Engine engine) {
        mapableEntities = engine.getEntitiesFor(Family.all(MapComponent.class, TransformComponent.class).get());
        player = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
        killableEntities = engine.getEntitiesFor(Family.all(HealthComponent.class, TransformComponent.class).exclude(ControlFocusComponent.class).get());
        
        
        menu = new Menu(false);
        
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
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
        
        Entity p = player.size() > 0 ? player.first() : null;
        miniMap.drawSpaceMap(shape, batch, p, mapableEntities);
        
        screenTransitionOverlay.render();
        
        //draw menu
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
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
        if (Gdx.input.isKeyJustPressed(SpaceProject.keyCFG.toggleHUD)) {
            drawHud = !drawHud;
            Gdx.app.log(this.getClass().getSimpleName(), "HUD: " + drawHud);
        }
        if (Gdx.input.isKeyJustPressed(SpaceProject.keyCFG.toggleEdgeMap)) {
            drawEdgeMap = !drawEdgeMap;
            Gdx.app.log(this.getClass().getSimpleName(), "Edge mapState: " + drawEdgeMap);
        }
        if (Gdx.input.isKeyJustPressed(SpaceProject.keyCFG.toggleSpaceMap)) {
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
        int barLength = SpaceProject.uiCFG.entityHPbarLength;
        int barWidth = SpaceProject.uiCFG.entityHPbarWidth;
        int yOffset = SpaceProject.uiCFG.entityHPbarYOffset;
        
        for (Entity entity : killableEntities) {
            Vector3 pos = cam.project(new Vector3(Mappers.transform.get(entity).pos.cpy(), 0));
            HealthComponent health = Mappers.health.get(entity);
            
            
            //ignore full health
            if (!SpaceProject.uiCFG.renderFullHealth && health.health == health.maxHealth) {
                continue;
            }
            
            //background
            shape.setColor(SpaceProject.uiCFG.entityHPbarBackground);
            shape.rect(pos.x - barLength / 2, pos.y + yOffset, barLength, barWidth);
            
            //health
            float ratio = health.health / health.maxHealth;
            shape.setColor(1 - ratio, ratio, 0, SpaceProject.uiCFG.entityHPbarOpacity); //creates color between red and green
            shape.rect(pos.x - barLength / 2, pos.y + yOffset, barLength * ratio, barWidth);
        }
        
    }
    
    /**
     * Draw the players health and ammo bar.
     */
    private void drawPlayerStatus() {
        int barWidth = SpaceProject.uiCFG.playerHPBarWidth;
        int barHeight = SpaceProject.uiCFG.playerHPBarHeight;
        int playerBarX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int playerHPBarY = SpaceProject.uiCFG.playerHPBarY;
        int playerAmmoBarY = playerHPBarY - barHeight - 1;
        
        if (player == null || player.size() == 0) return;
        
        //draw health bar
        HealthComponent health = Mappers.health.get(player.first());
        if (health != null) {
            float ratioHP = health.health / health.maxHealth;
            shape.setColor(SpaceProject.uiCFG.entityHPbarBackground);
            shape.rect(playerBarX, playerHPBarY, barWidth, barHeight);
            shape.setColor(1 - ratioHP, ratioHP, 0, SpaceProject.uiCFG.entityHPbarOpacity);
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
            shape.setColor(SpaceProject.uiCFG.entityHPbarBackground);
            shape.rect(playerBarX, playerAmmoBarY, barWidth, barHeight);
            shape.setColor(SpaceProject.uiCFG.playerAmmoBarColor);
            shape.rect(playerBarX, playerAmmoBarY, barWidth * ratioAmmo, barHeight);
            
            
            for (int i = 0; i < cannon.maxAmmo; i++) {
                int x = playerBarX + (i * barWidth / cannon.maxAmmo);
                //draw recharge bar
                if (i == cannon.curAmmo) {
                    shape.setColor(SpaceProject.uiCFG.playerAmmoBarRechargeColor);
                    shape.rect(x, playerAmmoBarY, barWidth / cannon.maxAmmo * cannon.timerRechargeRate.ratio(), barHeight);
                }
                //draw divisions to mark individual ammo
                if (i > 0) {
                    shape.setColor(Color.BLACK);
                    shape.rectLine(x, playerAmmoBarY + barHeight, x, playerAmmoBarY, 3);
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
     * TODO: move these values into MapComponent or a config file
     */
    private void drawEdgeMap() {
        float markerSmall = 3.5f; //min marker size
        float markerLarge = 8; //max marker size
        float distSmall = 600; //distance when marker is small
        float distLarge = 150; //distance when marker is large
        
        int padding = (int) (markerLarge + 4); //how close to draw from edge of screen (in pixels)
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int verticalEdge = (height - padding * 2) / 2;
        int horizontalEdge = (width - padding * 2) / 2;
        
        boolean drawBorder = false;
        if (drawBorder) {
            shape.setColor(Color.BLACK);
            shape.line(padding, padding, padding, height - padding);//left
            shape.line(width - padding, padding, width - padding, height - padding);//right
            shape.line(padding, padding, width - padding, padding);//bottom
            shape.line(padding, height - padding, width - padding, height - padding);//top
        }
    
        Vector3 topLeft = cam.unproject(new Vector3(0, 0, 0));
        Vector3 bottomRight = cam.unproject(new Vector3(width, height, 0));

        for (Entity mapable : mapableEntities) {
            MapComponent map = Mappers.map.get(mapable);
            Vector2 pos = Mappers.transform.get(mapable).pos.cpy();
            Vector3 screenPos = new Vector3(pos, 0);
            
            if (screenPos.dst(MyScreenAdapter.cam.position) > map.distance) {
                continue;
            }
            
            if (pos.x > topLeft.x && pos.x < bottomRight.x
                    && pos.y < topLeft.y && pos.y > bottomRight.y) {
                continue;
            }
            
            //set entity co'ords relative to center of screen
            screenPos.x -= MyScreenAdapter.cam.position.x;
            screenPos.y -= MyScreenAdapter.cam.position.y;
            
            
            //position to draw marker
            float markerX, markerY;
            
            //calculate slope of line (y = mx+b)
            float slope = screenPos.y / screenPos.x;
            
            //calculate where to position the marker
            if (screenPos.y < 0) {
                //top
                markerX = -verticalEdge / slope;
                markerY = -verticalEdge;
            } else {
                //bottom
                markerX = verticalEdge / slope;
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
            TextureComponent tex = Mappers.texture.get(mapable);
            float dist = MyMath.distance(pos.x, pos.y, cam.position.x, cam.position.y);
            if (tex != null) {
                dist -= Math.max(tex.texture.getWidth(), tex.texture.getHeight()) / 2.0f * tex.scale;
            }
            float distClamp = MathUtils.clamp((dist-distLarge)/(distSmall-distLarge), 0, 1);
            float sizeInterp = Interpolation.pow3In.apply(1-distClamp);
            float size = MathUtils.clamp((sizeInterp * (markerLarge-markerSmall)) + markerSmall, markerSmall, markerLarge);
            
            //draw marker
            shape.setColor(map.color);
            shape.circle(markerX, markerY, size);
			
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
    
    public ScreenTransitionOverlay getScreenTransitionOverlay() {
        return screenTransitionOverlay;
    }
    
    @Override
    public void resize(int width, int height) {
        menu.setSize(Gdx.graphics.getWidth() - 150, Gdx.graphics.getHeight() - 150);
        stage.getViewport().update(width, height, true);
        
        miniMap.updateMapPosition();
    }
    
}
