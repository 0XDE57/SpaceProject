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
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.config.UIConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.map.MapState;
import com.spaceproject.ui.map.MiniMap;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.IScreenResizeListener;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;


public class HUDSystem extends EntitySystem implements IRequireGameContext, IScreenResizeListener {
    
    private GameMenu gameMenu;
    
    //rendering
    private OrthographicCamera cam;
    private Matrix4 projectionMatrix;
    private ShapeRenderer shape;
    private SpriteBatch batch;
    
    //entity storage
    private ImmutableArray<Entity> mapableEntities;
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> killableEntities;
    private ImmutableArray<Entity> orbitEntities;
    
    private MiniMap miniMap;
    
    private boolean drawHud = true;
    private boolean drawEdgeMap = true;
    
    private final UIConfig uiCFG = SpaceProject.configManager.getConfig(UIConfig.class);
    private final KeyConfig keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
    
    public HUDSystem() {
        cam = MyScreenAdapter.cam;
        shape = MyScreenAdapter.shape;
        batch = MyScreenAdapter.batch;
        projectionMatrix = new Matrix4();
        
        GameScreen.getStage().addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (gameMenu.switchTabForKey(keycode)) {
                    if (!gameMenu.isVisible()) {
                        gameMenu.show();
                    }
                    return true;
                }
                return gameMenu.isVisible();
            }
        
            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                super.keyUp(event, keycode);
                return gameMenu.isVisible();
            }
        
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.MIDDLE) {
                    if (miniMap.getMapContainer().contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
                        if (miniMap.getState() != MapState.off) {
                            miniMap.resetMapScale();
                            return true;
                        }
                    }
                }
                
                super.touchDown(event, x, y, pointer, button);
                return gameMenu.isVisible();
            }
    
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                if (getMiniMap().scrolled(amountX, amountY)) {
                    return true;
                }
                
                return super.scrolled(event, x, y, amountX, amountY);
            }
        });
        
        miniMap = new MiniMap();
    }
    
    
    @Override
    public void initContext(GameScreen game) {
        gameMenu = new GameMenu(game, false);
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        mapableEntities = engine.getEntitiesFor(Family.all(MapComponent.class, TransformComponent.class).get());
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
        killableEntities = engine.getEntitiesFor(Family.all(HealthComponent.class, TransformComponent.class).exclude(ControlFocusComponent.class).get());
        orbitEntities = engine.getEntitiesFor(Family.all(TransformComponent.class).get());
    }
    
    @Override
    public void update(float delta) {
        checkInput();
    
        if (drawHud) {
            if (GameScreen.inSpace()) {
                drawOrbitPath(false);
            }
        
            drawHUD();
        
            if (miniMap.getState() != MapState.off) {
                Entity p = players.size() > 0 ? players.first() : null;
                miniMap.drawMiniMap(shape, batch, p, mapableEntities);
            }
        }
    
        //TODO: temporary fix. engine system priority....
        MobileInputSystem mobileUI = getEngine().getSystem(MobileInputSystem.class);
        if (mobileUI != null)
            mobileUI.drawControls();
    }
    
    private void drawHUD() {
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
    }
    
    private void drawOrbitPath(boolean showSyncedPos) {
        if (cam.zoom <= uiCFG.lodShowOrbitPath / uiCFG.orbitFadeFactor) {
            return;
        }
        
        float alpha = MathUtils.clamp((cam.zoom / uiCFG.lodShowOrbitPath / uiCFG.orbitFadeFactor), 0, 1);
        uiCFG.orbitObjectColor.a = alpha;
        uiCFG.orbitSyncPosColor.a = alpha;
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shape.begin(ShapeType.Line);
        
        for (Entity entity : orbitEntities) {
            
            OrbitComponent orbit = Mappers.orbit.get(entity);
            if (orbit != null) {
                TransformComponent entityPos = Mappers.transform.get(entity);
                
                if (orbit.parent != null) {
                    TransformComponent parentPos = Mappers.transform.get(orbit.parent);
                    
                    if (showSyncedPos) {
                        //synced orbit position (where the object should be)
                        Vector2 orbitPos = OrbitSystem.getTimeSyncedPos(orbit, GameScreen.getGameTimeCurrent());
                        shape.setColor(uiCFG.orbitSyncPosColor);
                        shape.line(parentPos.pos.x, parentPos.pos.y, orbitPos.x, orbitPos.y);
                    }
                    
                    //actual position
                    shape.setColor(uiCFG.orbitObjectColor);
                    shape.circle(parentPos.pos.x, parentPos.pos.y, orbit.radialDistance);
                    shape.line(parentPos.pos.x, parentPos.pos.y, entityPos.pos.x, entityPos.pos.y);
                }
                
                TextureComponent tex = Mappers.texture.get(entity);
                if (tex != null) {
                    float radius = tex.texture.getWidth() * 0.5f * tex.scale;
                    Vector2 orientation = MyMath.vector(entityPos.rotation, radius).add(entityPos.pos);
                    shape.setColor(uiCFG.orbitObjectColor);
                    shape.line(entityPos.pos.x, entityPos.pos.y, orientation.x, orientation.y);
                    shape.circle(entityPos.pos.x, entityPos.pos.y, radius);
                }
            }
        }
        
        shape.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    private void checkInput() {
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleHUD)) {
            drawHud = !drawHud;
            Gdx.app.log(this.getClass().getSimpleName(), "HUD: " + drawHud);
        }
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleEdgeMap)) {
            drawEdgeMap = !drawEdgeMap;
            Gdx.app.log(this.getClass().getSimpleName(), "Edge mapState: " + drawEdgeMap);
        }
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleSpaceMap)) {
            miniMap.cycleMapState();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            miniMap.cycleMiniMapPosition();
        }
    }
    
    private void drawHealthBars() {
        //bar dimensions
        int barLength = uiCFG.entityHPbarLength;
        int barWidth = uiCFG.entityHPbarWidth;
        int yOffset = uiCFG.entityHPbarYOffset;
        
        for (Entity entity : killableEntities) {
            Vector3 pos = cam.project(new Vector3(Mappers.transform.get(entity).pos.cpy(), 0));
            HealthComponent health = Mappers.health.get(entity);
            
            //ignore full health
            if (!uiCFG.renderFullHealth && health.health == health.maxHealth) {
                continue;
            }
            
            //background
            shape.setColor(uiCFG.entityHPbarBackground);
            shape.rect(pos.x - barLength * 0.5f, pos.y + yOffset, barLength, barWidth);
            
            //health
            float ratio = health.health / health.maxHealth;
            shape.setColor(1 - ratio, ratio, 0, uiCFG.entityHPbarOpacity); //creates color between red and green
            shape.rect(pos.x - barLength * 0.5f, pos.y + yOffset, barLength * ratio, barWidth);
        }
    }
    
    //region player status
    private void drawPlayerStatus() {
        if (players == null || players.size() == 0) {
            return;
        }
        
        int barWidth = uiCFG.playerHPBarWidth;
        int barHeight = uiCFG.playerHPBarHeight;
        int playerBarX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int playerHPBarY = uiCFG.playerHPBarY;
        int playerAmmoBarY = playerHPBarY - barHeight - 1;
        int playerHyperBarY = playerHPBarY + barHeight + 1;
        Entity playerEntity = players.first();
        
        drawPlayerHealth(playerEntity, playerBarX, playerHPBarY, barWidth, barHeight);
        drawPlayerShield(playerEntity, playerBarX, playerHPBarY, barWidth, barHeight);
        drawPlayerAmmoBar(playerEntity, playerBarX, playerAmmoBarY, barWidth, barHeight);
        drawHyperDriveBar(playerEntity, playerBarX, playerHyperBarY, barWidth, barHeight);

		/*
		//border
		shape.setColor(new Color(0.1f, 0.63f, 0.88f, 1f));
		int thickness = 1;
		shape.rectLine(playerBarX, playerHPBarY+barHeight, playerBarX+barWidth, playerHPBarY+barHeight, thickness);//top
		shape.rectLine(playerBarX, playerHPBarY-barHeight, playerBarX+barWidth, playerHPBarY-barHeight,thickness);//bottom
		shape.rectLine(playerBarX, playerHPBarY+barHeight, playerBarX, playerHPBarY-barHeight, thickness);//left
		shape.rectLine(playerBarX+barWidth, playerHPBarY+barHeight, playerBarX+barWidth, playerHPBarY-barHeight, thickness);//right
		*/
    }
    
    private void drawPlayerHealth(Entity playerEntity, int playerBarX, int playerHPBarY, int barWidth, int barHeight) {
        HealthComponent health = Mappers.health.get(playerEntity);
        if (health != null) {
            shape.setColor(uiCFG.entityHPbarBackground);
            shape.rect(playerBarX, playerHPBarY, barWidth, barHeight);
    
            float ratioHP = health.health / health.maxHealth;
            shape.setColor(1 - ratioHP, ratioHP, 0, uiCFG.entityHPbarOpacity);
            shape.rect(playerBarX, playerHPBarY, barWidth * ratioHP, barHeight);
        }
    }
    
    private void drawPlayerShield(Entity playerEntity, int playerBarX, int playerHPBarY, int barWidth, int barHeight) {
        ShieldComponent shield = Mappers.shield.get(playerEntity);
        if (shield != null) {
            float ratioShield = shield.radius / shield.maxRadius;
            if (shield.isActive) {
                shape.setColor(shield.color);
            } else {
                shape.setColor(shield.color.r, shield.color.g, shield.color.b, 0.25f);
            }
            shape.rect(playerBarX, playerHPBarY, barWidth * ratioShield, barHeight);
        }
    }
    
    private void drawPlayerAmmoBar(Entity playerEntity, int playerBarX, int playerAmmoBarY, int barWidth, int barHeight) {
        CannonComponent cannon = Mappers.cannon.get(playerEntity);
        if (cannon != null) {
            float ratioAmmo = (float) cannon.curAmmo / (float) cannon.maxAmmo;
            shape.setColor(uiCFG.entityHPbarBackground);
            shape.rect(playerBarX, playerAmmoBarY, barWidth, barHeight);
            shape.setColor(uiCFG.playerAmmoBarColor);
            shape.rect(playerBarX, playerAmmoBarY, barWidth * ratioAmmo, barHeight);
            
            
            for (int i = 0; i < cannon.maxAmmo; i++) {
                int x = playerBarX + (i * barWidth / cannon.maxAmmo);
                //draw recharge bar
                if (i == cannon.curAmmo) {
                    shape.setColor(uiCFG.playerAmmoBarRechargeColor);
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
    }
    
    private void drawHyperDriveBar(Entity playerEntity, int playerBarX, int playerHyperBarY, int barWidth, int barHeight) {
        HyperDriveComponent hyperDrive = Mappers.hyper.get(playerEntity);
        if (hyperDrive == null) {
            return;
        }
    
        shape.setColor(Color.BLUE);
        if (hyperDrive.isActive) {
            shape.rect(playerBarX, playerHyperBarY, barWidth, barHeight);
        } else {
            float ratio = MathUtils.clamp(0, (1 - hyperDrive.coolDownTimer.ratio()) * barWidth, barWidth);
            shape.rect(playerBarX, playerHyperBarY, ratio, barHeight);
        }
    }
    //endregion
    
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
    
    public GameMenu getGameMenu() {
        return gameMenu;
    }
    
    @Override
    public void resize(int width, int height) {
        gameMenu.resetPosition();
        
        miniMap.updateMapPosition();
    }
    
}
