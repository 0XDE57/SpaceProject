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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.*;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.config.UIConfig;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.ui.map.MapState;
import com.spaceproject.ui.map.MiniMap;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.ui.menu.SpaceStationMenu;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.IScreenResizeListener;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class HUDSystem extends EntitySystem implements IRequireGameContext, IScreenResizeListener, Disposable {

    public static class DamageText implements Pool.Poolable {
        public float damage;
        public float x, y;
        public long timestamp;
        public boolean project;

        public void init(float x, float y, float damage, long lastHitTime, boolean project) {
            this.x = x;
            this.y = y;
            this.damage = damage;
            this.timestamp = lastHitTime;
            this.project = project;
        }

        @Override
        public void reset() {}

    }

    private static final Array<DamageText> activeNumbers = new Array<>();
    private static final Pool<DamageText> numbersPool = Pools.get(DamageText.class);
    public static boolean showDamageNumbers = false;
    public static int damageTime = 750;
    public static int activePeak;
    StringBuilder infoString = new StringBuilder();

    class CreditsMarker {//can this fit into the same damage pool? it really doesn't need to...
        final int value;
        Vector2 location;
        SimpleTimer animTimer;
        final Color color;
        CreditsMarker(int value, Vector2 pos, Color color) {
            this.value = value;
            Vector3 screenPos = cam.project(new Vector3(pos.cpy(), 0));//copy
            location = new Vector2(screenPos.x, screenPos.y);//new
            animTimer = new SimpleTimer(1000, true);//new -> float timestamp
            this.color = color;
        }
    }
    private final List<CreditsMarker> markers;

    private final UIConfig uiCFG = SpaceProject.configManager.getConfig(UIConfig.class);
    private final KeyConfig keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
    
    private GameMenu gameMenu;
    private VisWindow stationMenu;

    //rendering
    private final OrthographicCamera cam;
    private final Matrix4 projectionMatrix;
    private final ShapeRenderer shape;
    private final SpriteBatch batch;
    private final BitmapFont font, subFont, inventoryFont;
    private final GlyphLayout layout;
    private final Vector3 tempProj = new Vector3();
    private final Color cacheColor = new Color();

    //entity storage
    private ImmutableArray<Entity> mapableEntities;
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> killableEntities;

    private final Vector3 topLeft = new Vector3();
    private final Vector3 bottomRight = new Vector3();
    private final Vector3 screenPos = new Vector3();
    private final Vector2 focusedPos = new Vector2();
    private float focusedDist;
    
    private MiniMap miniMap;
    
    private static boolean drawHud = true;
    private boolean drawEdgeMap = true;

    enum SpecialState {
        off, docked, landing, launching, destroyed;
    }

    float statusAnim = 0;
    float damageAnim = 0;
    
    public HUDSystem() {
        cam = MyScreenAdapter.cam;
        shape = MyScreenAdapter.shape;
        batch = MyScreenAdapter.batch;
        projectionMatrix = new Matrix4();
        layout = new GlyphLayout();

        miniMap = new MiniMap();
        markers = new ArrayList<>();

        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 26;
        parameter.borderColor = Color.BLACK;
        parameter.borderWidth = 3;
        font = FontLoader.createFont(FontLoader.fontPressStart, parameter);
    
        FreeTypeFontGenerator.FreeTypeFontParameter parameter2 = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter2.size = 20;
        parameter2.borderColor = Color.BLACK;
        parameter2.borderWidth = 3;
        subFont = FontLoader.createFont(FontLoader.fontPressStart, parameter2);
    
        FreeTypeFontGenerator.FreeTypeFontParameter inventoryParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        inventoryParam.size = 12;
        inventoryParam.borderColor = Color.BLACK;
        inventoryParam.borderWidth = 1;
        inventoryFont = FontLoader.createFont(FontLoader.fontPressStart, inventoryParam);

        GameScreen.getStage().addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                //todo alternatively?
                //if (E ) and at station
                //    stationmenu.show
                if (gameMenu.switchTabForKey(keycode)) {
                    if (!gameMenu.isVisible()) {
                        gameMenu.show();
                    }
                    return true;
                }
                return super.keyDown(event, keycode);
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
    }
    
    @Override
    public void initContext(GameScreen game) {
        gameMenu = new GameMenu(game, false);
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        mapableEntities = engine.getEntitiesFor(Family.all(MapComponent.class, TransformComponent.class).get());
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
        killableEntities = engine.getEntitiesFor(Family.all(HealthComponent.class, TransformComponent.class)
                .exclude(ControlFocusComponent.class, AsteroidComponent.class).get());
    }
    
    @Override
    public void update(float deltaTime) {
        checkInput();
    
        if (drawHud) {
            statusAnim += 4f * deltaTime;
            drawHUD(deltaTime);
        
            if (miniMap.getState() != MapState.off) {
                Entity p = players.size() > 0 ? players.first() : null;
                miniMap.drawMiniMap(shape, batch, p, mapableEntities);
            }
        }
    
        //TODO: temporary fix. engine system priority....
        //always draw even if hud off
        MobileInputSystem mobileUI = getEngine().getSystem(MobileInputSystem.class);
        if (mobileUI != null)
            mobileUI.drawControls();
    }

    public boolean isDraw() {
        return drawHud;
    }
    
    private void drawHUD(float deltaTime) {
        //set projection matrix so things render using correct coordinates
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(projectionMatrix);
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shape.begin(ShapeType.Filled);

        CameraSystem camSys = getEngine().getSystem(CameraSystem.class);
        drawZoomLevelIndicator(camSys);

        Entity player = null;
        if (players.size() > 0) {
            player = players.first();
        }
        drawPlayerStatus(player, camSys);
        
        if (drawEdgeMap) {
            drawEdgeMap();
        }
        
        drawHealthBars();

        //draw background for state message
        if (messageState != SpecialState.off) {
            drawMessageBacking(player);
        }
        if (messageState != SpecialState.docked) {
            if (stationMenu != null) {
                stationMenu.fadeOut();
                stationMenu = null;
            }
        }

        shape.end();


        batch.begin();
        drawStatusInfo(player, uiCFG.playerHPBarWidth, uiCFG.playerHPBarHeight, uiCFG.playerHPBarY + 5);
        drawInventory(player, 20, 30);

        drawCreditMarkers(deltaTime);
        drawDamageText();

        //draw special state: hyper or landing / launching
        drawSpecialStateMessage(player);

        if (player == null) {
            ImmutableArray<Entity> respawnEntities = getEngine().getEntitiesFor(Family.all(RespawnComponent.class).get());
            if (respawnEntities.size() > 0) {
                RespawnComponent respawn = Mappers.respawn.get(respawnEntities.first());
                drawHint(respawn.reason);
            }
        } else {
            ControllableComponent control = Mappers.controllable.get(player);
            if (control != null && control.canTransition && (Mappers.screenTrans.get(player) == null)) {
                String key = Input.Keys.toString(SpaceProject.configManager.getConfig(KeyConfig.class).interact);
                String input = (getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus() ? "D-Pad Down" : key).toUpperCase();
                if (GameScreen.inSpace()) {
                    drawHint("press [" + input + "] to land");
                } else {
                    if (Mappers.vehicle.get(player) != null) {
                        drawHint("press [" + input + "] to take off");
                    }
                }
            }
        }

        Entity focused = getEngine().getSystem(GridRenderSystem.class).closestFacing;
        if (focused != null) {
            PlanetComponent planet = Mappers.planet.get(focused);
            if (planet != null) {
                TransformComponent transform = Mappers.transform.get(focused);
                tempProj.set(transform.pos.x, transform.pos.y, 0);
                Vector3 screenPos = MyScreenAdapter.cam.project(tempProj);
                float offset = layout.height * 1.5f;
                float alpha = MathUtils.clamp((cam.zoom / 150 / 2), 0, 1);
                inventoryFont.setColor(1, 1, 1, alpha);
                layout.setText(inventoryFont, "<planet name>");
                inventoryFont.draw(batch, layout, screenPos.x - layout.width*0.5f, screenPos.y + offset);
                layout.setText(inventoryFont, "atmosphere: <unknown>");
                inventoryFont.draw(batch, layout, screenPos.x - layout.width*0.5f, screenPos.y);
                layout.setText(inventoryFont, "size: " + planet.mapSize);
                inventoryFont.draw(batch, layout, screenPos.x - layout.width*0.5f, screenPos.y - offset);//replace with mass?
            }
            StarComponent star = Mappers.star.get(focused);
            if (star != null) {
                TransformComponent transform = Mappers.transform.get(focused);
                tempProj.set(transform.pos.x, transform.pos.y, 0);
                Vector3 screenPos = MyScreenAdapter.cam.project(tempProj);
                float offset = layout.height * 1.5f;
                float alpha = MathUtils.clamp((cam.zoom / 150 / 2), 0, 1);
                inventoryFont.setColor(1, 1, 1, alpha);
                layout.setText(inventoryFont, "<star type>" /*+ star.StellarClass*/);
                inventoryFont.draw(batch, layout, screenPos.x - layout.width*0.5f, screenPos.y + offset);
                layout.setText(inventoryFont, "temperature: " + (int)star.temperature + "k");
                inventoryFont.draw(batch, layout, screenPos.x - layout.width*0.5f, screenPos.y);
            }
        }

        if (focusedDist > 0) {
            inventoryFont.setColor(1, 1, 1, 1);
            layout.setText(inventoryFont, " " + (int)focusedDist );
            inventoryFont.draw(batch, layout, focusedPos.x, focusedPos.y+1);
        }

        //drawHint("stars are hot");
        //drawHint("an object in motion remains in motion");
        float warningDist = 200000;
        warningDist = warningDist * warningDist;
        if (cam.position.dst2(0,0,0) > warningDist) {
            drawHint("warning: broken physics ahead " + MyMath.formatVector3as2(cam.position, 1));
        }
    
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawZoomLevelIndicator(CameraSystem camSys) {
        if (MathUtils.isEqual(cam.zoom, CameraSystem.getZoomForLevel(camSys.getZoomLevel()), 0.1f)
                && camSys.zoomChangeTimer.canDoEvent()) return;

        int width = uiCFG.playerHPBarWidth * 2;
        int x = Gdx.graphics.getWidth() / 2 - width / 2;
        int y = Gdx.graphics.getHeight() - 30;
        int thickness = 2;

        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rectLine(x, y, x + width, y, thickness); //base line

        //levels
        int sliderHeight = 30;
        float maxZoom = CameraSystem.getZoomForLevel(camSys.getMaxZoomLevel());
        for (byte l = 0; l <= camSys.getMaxZoomLevel(); l++) {
            float r = CameraSystem.getZoomForLevel(l) / maxZoom;
            shape.rect(x + (r * width), y - (sliderHeight * 0.5f), thickness, sliderHeight);
        }

        //target
        shape.setColor(Color.WHITE);
        float rr = CameraSystem.getZoomForLevel(camSys.getZoomLevel()) / maxZoom;
        shape.rect(x + (rr * width), y - (sliderHeight * 0.5f), thickness, sliderHeight);

        //current
        shape.setColor(uiCFG.uiBaseColor);
        float ratio = cam.zoom / maxZoom;
        shape.rect(x + (ratio * width), y - (sliderHeight * 0.5f), thickness, sliderHeight);
    }

    boolean interactHold;
    private void drawMessageBacking(Entity player) {
        int offset = 50;
        int messageHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight()/3)) - offset;
        float height =  40 + offset;
        shape.rect(0, messageHeight, Gdx.graphics.getWidth(), height, Color.CLEAR, Color.CLEAR, Color.DARK_GRAY, Color.DARK_GRAY);
        Color color = messageState == SpecialState.destroyed ? Color.RED : uiCFG.uiBaseColor;
        shape.setColor(color);
        shape.rectLine(0, messageHeight + height, Gdx.graphics.getWidth(), messageHeight + height, 3);
        shape.rectLine(0, messageHeight, Gdx.graphics.getWidth(), messageHeight, 1);

        int timerHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight()/3) - 6);
        float halfWidth = Gdx.graphics.getWidth()*0.5f;

        //draw respawn line
        if (messageState == SpecialState.destroyed) {
            ImmutableArray<Entity> respawnEntities = getEngine().getEntitiesFor(Family.all(RespawnComponent.class).get());
            if (respawnEntities.size() > 0) {
                RespawnComponent respawn = Mappers.respawn.get(respawnEntities.first());
                if (respawn.spawn == RespawnComponent.AnimState.pause || respawn.spawn == RespawnComponent.AnimState.pan) {
                    float ratio = 1-respawn.timeout.ratio();
                    shape.setColor(Color.WHITE);
                    shape.rectLine(halfWidth - (halfWidth*ratio), timerHeight, halfWidth + (ratio * halfWidth), timerHeight, 2);
                }
            }
        }

        if (messageState == SpecialState.docked) {
            if (player != null) {
                ControllableComponent control = Mappers.controllable.get(player);
                if (control.interact) {
                    if (!interactHold) {
                        if (stationMenu == null) {
                            ControllerMenuStage controllerMenuStage = GameScreen.getStage();
                            stationMenu = (VisWindow) SpaceStationMenu.SpaceStationMenu(controllerMenuStage, player, getEngine());
                        } else {
                            stationMenu.remove();
                        }
                    }
                    interactHold = true;
                } else {
                    interactHold = false;
                }
            }
        }

        if (stationMenu != null && stationMenu.getParent() == null) {
            //System.out.println(stationMenu.isVisible() + ", " + stationMenu.getParent());
            stationMenu = null;
        }
    }

    private void checkInput() {
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleHUD)) {//todo: kill key toggle move to debug
            drawHud = !drawHud;
            Gdx.app.log(getClass().getSimpleName(), "HUD: " + drawHud);
        }
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleEdgeMap)) {//kill or move to UI option
            drawEdgeMap = !drawEdgeMap;
            Gdx.app.log(getClass().getSimpleName(), "Edge mapState: " + drawEdgeMap);
        }
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleSpaceMap)) {
            miniMap.cycleMapState();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) { //kill or move to UI option
            miniMap.cycleMiniMapPosition();
        }
        if (Gdx.app.getInput().isKeyJustPressed(Input.Keys.Y)) {//kill our move to debug menu
            if (players.size() > 0) {
                CargoComponent cargo = Mappers.cargo.get(players.first());
                if (cargo != null) {
                    int amount = 1000;
                    for (ItemComponent.Resource resource : ItemComponent.Resource.values()) {
                        int id = resource.getId();
                        int currentQuantity = 0;
                        if (cargo.inventory.containsKey(id)) {
                            currentQuantity = cargo.inventory.get(id);
                        }
                        cargo.inventory.put(id, currentQuantity + amount);
                    }
                    cargo.lastCollectTime = GameScreen.getGameTimeCurrent();
                    Gdx.app.debug(getClass().getSimpleName(), "cheat! add items");
                }
            }
        }
    }
    
    private void drawHealthBars() {
        //bar dimensions
        int barLength = uiCFG.entityHPbarLength;
        int barWidth = uiCFG.entityHPbarWidth;
        int yOffset = uiCFG.entityHPbarYOffset;
        
        for (Entity entity : killableEntities) {
            HealthComponent health = Mappers.health.get(entity);
            //ignore full health
            if (!uiCFG.renderFullHealth && health.health == health.maxHealth) {
                continue;
            }

            tempProj.set(Mappers.transform.get(entity).pos, 0);
            cam.project(tempProj);
            
            //background
            shape.setColor(uiCFG.entityHPbarBackground);
            shape.rect(tempProj.x - barLength * 0.5f, tempProj.y + yOffset, barLength, barWidth);
            
            //health
            float ratio = health.health / health.maxHealth;
            shape.setColor(1 - ratio, ratio, 0, uiCFG.entityHPbarOpacity); //creates color between red and green
            shape.rect(tempProj.x - barLength * 0.5f, tempProj.y + yOffset, barLength * ratio, barWidth);
        }
    }
    
    //region player status
    SpecialState messageState = SpecialState.off;
    private void drawSpecialStateMessage(Entity player) {
        messageState = SpecialState.off;
        if (player == null) {
            messageState = SpecialState.destroyed;
        } else {
            ScreenTransitionComponent trans = Mappers.screenTrans.get(player);
            if (trans != null) {
                if (trans.landStage != null) {
                    messageState = SpecialState.landing;
                }
                if (trans.takeOffStage != null) {
                    messageState = SpecialState.launching;
                }
            }
            ControllableComponent control = Mappers.controllable.get(player);
            if (control != null && !control.activelyControlled) {
                messageState = SpecialState.docked;
            }
        }

        switch (messageState) {
            case docked:
                String key = Input.Keys.toString(SpaceProject.configManager.getConfig(KeyConfig.class).interact);
                boolean controllerHasFocus = getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus();
                if (stationMenu == null) {
                    String input = (controllerHasFocus ? "D-Pad Down" : key).toUpperCase();
                    drawHint("[" + input + "] open shop");
                } else {
                    drawHint("[" + (controllerHasFocus ? "B" : key).toUpperCase() + "] close shop");
                }
                String port = Mappers.docked.get(player).dockID;
                layout.setText(font, "[ DOCKED: port " + port + " ]");
            break;
            case landing:
                drawHint("<planet name goes here>");
                layout.setText(font, "[ LANDING ]");
                break;
            case launching:
                drawHint("entering space");
                layout.setText(font, "[ LAUNCHING ]");
                break;
            case destroyed:
                layout.setText(font, "[ DESTROYED ]");
                break;
            case off:
                return;
        }

        float ratio = 1 + (float) Math.sin(statusAnim);
        if (messageState == SpecialState.destroyed) {
            cacheColor.set(0, 0, 0, 1).lerp(Color.RED, ratio);
        } else {
            cacheColor.set(Color.GOLD).lerp(Color.CYAN, ratio);
        }
        font.setColor(cacheColor);
        
        float centerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        float messageHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight()/3f) + layout.height);
        font.draw(batch, layout, centerX, messageHeight);
    }

    private void drawHint(String text) {
        float ratio = 1 + (float) Math.sin(statusAnim * 0.1f);
        cacheColor.set(0, 1, 0, 1).lerp(Color.PURPLE, ratio);
        subFont.setColor(cacheColor);
        layout.setText(subFont, text);
        
        float centerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        float messageHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight()/3f) + layout.height);
        messageHeight -= layout.height * 2;
        subFont.draw(batch, layout, centerX, messageHeight);
    }
    
    private void drawPlayerStatus(Entity entity, CameraSystem cam) {
        if (entity == null) return;
        
        int barWidth = uiCFG.playerHPBarWidth;
        int barHeight = uiCFG.playerHPBarHeight;
        int barX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int healthBarY = uiCFG.playerHPBarY;
        int ammoBarY = healthBarY - barHeight - 1;
        int hyperBarY = healthBarY + barHeight + 1;
        
        drawPlayerVelocity(entity, barX, hyperBarY, barWidth, barHeight);
        drawHyperDriveBar(entity, barX, hyperBarY, barWidth, barHeight);

        if (!GameScreen.isPaused()) {
            if (cam.getZoomLevel() == cam.getMaxZoomLevel()) return;

            if (GameScreen.isHyper()) return;
        }

        drawPlayerHealth(entity, barX, healthBarY, barWidth, barHeight);
        drawPlayerShield(entity, barX, healthBarY, barWidth, barHeight);
        drawPlayerAmmoBar(entity, barX, ammoBarY, barWidth, barHeight);
    }

    private void drawPlayerHealth(Entity entity, float x, float y, float width, float height) {
        HealthComponent health = Mappers.health.get(entity);
        if (health == null) return;
        
        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rect(x, y, width, height);
    
        float ratioHP = health.health / health.maxHealth;
        boolean warning = ratioHP < 0.25f;
        boolean hit = GameScreen.getGameTimeCurrent() - health.lastHitTime < 1000;
        if (hit || warning) {
            if (warning) {
                damageAnim += 10 * Gdx.graphics.getDeltaTime();
                float alpha = (float) Math.abs(Math.sin(damageAnim));
                shape.setColor(1, 0, 0, alpha);
            } else {
                shape.setColor(1, 0, 0, 1);
            }
            //border
            int thickness = 2;
            shape.rectLine(x, y+height, x+width, y+height, thickness);//top
            shape.rectLine(x, y, x+width, y, thickness);//bottom
            shape.rectLine(x, y+height, x, y, thickness);//left
            shape.rectLine(x+width, y+height, x+width, y, thickness);//right
        }
        if (!hit) {
            shape.setColor(1 - ratioHP, ratioHP, 0, uiCFG.entityHPbarOpacity);
        }

        shape.rect(x, y, width * ratioHP, height);
    }

    private void drawPlayerShield(Entity entity, float x, float y, float width, float height) {
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield == null || shield.state == ShieldComponent.State.off) {
            return;
        }
    
        //if shield not engaged, render bar half height
        float halfHeight = height * 0.5f;
        if (shield.state != ShieldComponent.State.on) {
            height = halfHeight;
            y += halfHeight * 0.5f;
        }
    
        float ratioShield = shield.radius / shield.maxRadius;
        if (shield.state == ShieldComponent.State.on) {
            long hitTime = 500;
            long timeSinceHit = GameScreen.getGameTimeCurrent() - shield.lastHit;
            if (timeSinceHit < hitTime) {
                float green = (float) timeSinceHit / hitTime;
                shape.setColor(0, 1-green, green, green);
            } else {
                shape.setColor(shield.heat, 0, 1, 1);
            }
        } else {
            shape.setColor(shield.heat, 0, 1, 0.25f + shield.heat);
        }
        if (shield.heat >= 0.95f) {
            shape.setColor(1, 0, 0, 1);
        }
        shape.rect(x, y, width * ratioShield, height);

        if (shield.state == ShieldComponent.State.on) {
            drawPlayerHealth(entity, x, y + halfHeight * 0.5f, width, halfHeight);
            shape.setColor(Color.BLUE);
            if (shield.heat >= 0.95f) {
                shape.setColor(1, 0, 0, 1);
            }
            int thickness = 2;
            shape.rectLine(x, y + height, x + width, y + height, thickness);//top
            shape.rectLine(x, y, x + width, y, thickness);//bottom
            shape.rectLine(x, y + height, x, y, thickness);//left
            shape.rectLine(x + width, y + height, x + width, y, thickness);//right
        }
    }
    
    private void drawPlayerAmmoBar(Entity entity, int x, int y, int width, int height) {
        drawCannonHeatBar(entity, x, y, width, height);
        drawGrowCannonAmmoBar(entity, x, y, width, height);
    }
    
    private void drawGrowCannonAmmoBar(Entity entity, int x, int y, int width, int height) {
        ChargeCannonComponent chargeCannon = Mappers.chargeCannon.get(entity);
        if (chargeCannon == null)  return;
        
        float ratioAmmo = chargeCannon.size / chargeCannon.maxSize;
        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rect(x, y, width, height);
        if (chargeCannon.isCharging) {
            shape.setColor(uiCFG.playerAmmoBarRechargeColor);
            if (chargeCannon.size == chargeCannon.maxSize) {
                shape.setColor(uiCFG.playerAmmoBarColor);
            }
            shape.rect(x, y, width * ratioAmmo, height);
        }
    }

    private void drawCannonHeatBar(Entity entity, int x, int y, int width, int height) {
        CannonComponent cannon = Mappers.cannon.get(entity);
        if (cannon == null) return;

        float heatRatio = cannon.heat;
        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rect(x, y, width, height);
        shape.setColor(uiCFG.playerAmmoBarColor);
        shape.rect(x, y, width * heatRatio, height);
    }
    
    private void drawPlayerVelocity(Entity entity, int x, int y, int width, int height) {
        shape.setColor(Color.BLACK);
        
        ControllableComponent control = Mappers.controllable.get(entity);
        if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight || control.boost) {
            shape.setColor(control.boost ? uiCFG.engineBoost : uiCFG.engineFire);
        }
        shape.rect(x, y + (height*0.5f), width, 1);
        
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics == null) {
            return;
        }

        float velocity = physics.body.getLinearVelocity().len() / Box2DPhysicsSystem.getVelocityLimit();
        float barRatio = MathUtils.clamp(velocity * width, 0,  width);
        float center = (width * 0.5f) - (barRatio * 0.5f);
        shape.rect(x + center, y, barRatio, height);
    }

    private void drawHyperDriveBar(Entity entity, int x, int y, int width, int height) {
        HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
        if (hyperDrive == null) return;

        //if hyper drive not engaged, render bar half width
        if (hyperDrive.state != HyperDriveComponent.State.on) {
            int halfHeight = height / 2;
            height = halfHeight;
            y += halfHeight / 2;
        }
        
        shape.setColor(Color.WHITE);
        switch (hyperDrive.state) {
            case on:
                shape.rect(x, y, width, height);
                break;
            case charging:
                float charge = MathUtils.clamp(0, hyperDrive.chargeTimer.ratio() * width, width);
                float center = (width * 0.5f) - (charge * 0.5f);
                shape.rect(x + center, y, charge, height);
                break;
            case cooldown:
                float cooldown = MathUtils.clamp(0, (1 - hyperDrive.coolDownTimer.ratio()) * width, width);
                float centerD = (width * 0.5f) - (cooldown * 0.5f);
                shape.rect(x + centerD, y, cooldown, height);
                break;
        }
    }

    private void drawStatusInfo(Entity entity, int barWidth, int barHeight, int healthBarY) {
        if (entity == null) return;

        int barX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
            cacheColor.set(Color.GOLD).lerp(Color.CYAN, 1 + (float) Math.sin(statusAnim));
            layout.setText(inventoryFont, "[ HYPER-DRIVE ]", cacheColor, 0, Align.center, false);
            inventoryFont.draw(batch, layout, barX + layout.width - 37, healthBarY + barHeight + layout.height -1);
            return;
        }
        CameraSystem camSystem = getEngine().getSystem(CameraSystem.class);
        if (camSystem.getZoomLevel() == camSystem.getMaxZoomLevel()) {
            return;
        }

        //draw velocity
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            ControllableComponent control = Mappers.controllable.get(entity);
            if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight || control.boost) {
                inventoryFont.setColor(control.boost ? uiCFG.engineBoost : uiCFG.engineFire);
            } else {
                inventoryFont.setColor(Color.WHITE);
            }
            layout.setText(inventoryFont, " " + MyMath.round(physics.body.getLinearVelocity().len(), 1));
            inventoryFont.draw(batch, layout, barX + barWidth, healthBarY + barHeight + layout.height);
        }
        //draw HP
        HealthComponent health = Mappers.health.get(entity);
        if (health != null) {
            float ratio = health.health / health.maxHealth;
            Color color = ratio > 0.66 ? Color.GREEN : ratio > 0.33 ? Color.WHITE : Color.RED;
            if (GameScreen.getGameTimeCurrent() - health.lastHitTime < 1000) {
                color = Color.RED;
            }
            ShieldComponent shield = Mappers.shield.get(entity);
            if (shield != null) {
                //draw shield key
                String input = Input.Keys.toString(SpaceProject.configManager.getConfig(KeyConfig.class).activateShield);
                if (getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus()) {
                    input = "LT";
                }
                layout.setText(inventoryFont, "[" + input + "] ", Color.WHITE, 0, Align.right, false);
                inventoryFont.draw(batch, layout, barX, healthBarY + layout.height);
                if (shield.state == ShieldComponent.State.on) {
                    color = Color.BLUE;
                }
            }
            inventoryFont.setColor(color);
            inventoryFont.draw(batch, " " + MyMath.round(health.health, 1), barX + barWidth, healthBarY + layout.height);
        }

        VehicleComponent vehicleComponent = Mappers.vehicle.get(entity);
        if (vehicleComponent != null && vehicleComponent.tools.size() > 0) {
            inventoryFont.setColor(Color.WHITE);
            String key = Input.Keys.toString(SpaceProject.configManager.getConfig(KeyConfig.class).switchWeapon);
            String input = getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus() ? " [D-Pad Right] " : " [" + key + "] ";
            String toolText = vehicleComponent.currentTool.toString();
            if (vehicleComponent.currentTool == VehicleComponent.Tool.tractor) {
                toolText += ":" + Mappers.tractor.get(entity).mode.toString();
            }
            layout.setText(inventoryFont, toolText + input, Color.WHITE, 0, Align.right, false);
            inventoryFont.draw(batch, layout, barX, healthBarY - barHeight + layout.height - 3);
        }
    }
    
    private void drawInventory(Entity entity, float inventoryX, float inventoryY) {
        if (entity == null) return;
        CargoComponent cargo = Mappers.cargo.get(entity);
        if (cargo == null) return;

        long colorTime = 6000;
        long timeSinceCollect = GameScreen.getGameTimeCurrent() - cargo.lastCollectTime;
        float ratio = (float) timeSinceCollect / (float) colorTime;
        if (GameScreen.isPaused() || messageState == SpecialState.docked) {
            ratio = 0f;
        }
        inventoryFont.setColor(1, 1, 1, 1-ratio);
        inventoryFont.draw(batch, cargo.credits + " credits", inventoryX, inventoryY);
        int offsetY = 1;
        for (ItemComponent.Resource resource : ItemComponent.Resource.values()) {
            int id = resource.getId();
            if (cargo.inventory.containsKey(id)) {
                int quantity = cargo.inventory.get(id);
                Color color = resource.getColor();
                inventoryFont.setColor(color.r, color.g, color.b, 1-ratio);
                layout.setText(inventoryFont, quantity + " " + resource.name().toLowerCase());
                inventoryFont.draw(batch, layout, inventoryX, inventoryY + (layout.height * 1.5f * offsetY++));
            }
        }
    }

    private void drawDamageText() {
        for (Iterator<DamageText> iterator = activeNumbers.iterator(); iterator.hasNext();) {
            DamageText text = iterator.next();
            float x = text.x;
            float y = text.y;
            if (text.project) {
                tempProj.set(x, y, 0);
                MyScreenAdapter.cam.project(tempProj);
                x = tempProj.x;
                y = tempProj.y;
            }
            cacheColor.set(1, 0, 0, 1).lerp(Color.CLEAR, (float) (GameScreen.getGameTimeCurrent() - text.timestamp) / damageTime);
            inventoryFont.setColor(cacheColor);
            inventoryFont.draw(batch, "" + (int)text.damage, x, y);

            if (text.timestamp < GameScreen.getGameTimeCurrent() - damageTime) {
                iterator.remove();
                numbersPool.free(text);
            }
        }
    }

    public static void damageMarker(Vector2 pos, float damage, long lastHitTime) {
        if (!showDamageNumbers || !drawHud) return;
        //https://libgdx.com/wiki/articles/memory-management#object-pooling
        DamageText test = numbersPool.obtain();
        test.init(pos.x, pos.y, damage, lastHitTime, true);
        activeNumbers.add(test);
        activePeak = Math.max(activePeak, activeNumbers.size);
    }

    private void drawCreditMarkers(float deltaTime) {
        float velocityY = 35f;
        for (Iterator<CreditsMarker> iterator = markers.iterator(); iterator.hasNext();) {
            CreditsMarker marker = iterator.next();
            inventoryFont.setColor(marker.color.r, marker.color.g, marker.color.b, 1-marker.animTimer.ratio());
            inventoryFont.draw(batch, "+" + marker.value, marker.location.x, marker.location.y += velocityY * deltaTime);
            if (marker.animTimer.tryEvent()) {
                iterator.remove();
            }
        }
    }

    public void addCredits(int totalCredits, Vector2 pos, Color color) {
        markers.add(new CreditsMarker(totalCredits, pos, color));//todo: replace with pool
    }
    //endregion
    
    /**
     * Mark off-screen objects on edge of screen for navigation.
     * TODO: load star mapState markers based on point list instead of star entity for stars that aren't loaded yet
     * TODO: move these values into MapComponent or a config file
     */
    private void drawEdgeMap() {
        Entity closestApproaching = getEngine().getSystem(GridRenderSystem.class).closestVelocity;

        float markerSmall = 3.5f; //min marker size
        float markerLarge = 8; //max marker size
        float distSmall = 600; //distance when marker is small
        float distLarge = 200; //distance when marker is large
        
        int padding = (int) (markerLarge + 4); //how close to draw from edge of screen (in pixels)
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int verticalEdge = (height - padding * 2) / 2;
        int horizontalEdge = (width - padding * 2) / 2;
        
        //todo: add navigation point for current direction facing
        //todo: add navigation point for current velocity vector
        focusedDist = -1;
        topLeft.set(0, 0, 0);
        cam.unproject(topLeft);
        bottomRight.set(width, height, 0);
        cam.unproject(bottomRight);
        for (Entity mapable : mapableEntities) {
            MapComponent map = Mappers.map.get(mapable);
            Vector2 pos = Mappers.transform.get(mapable).pos;
            screenPos.set(pos, 0);
            
            if (screenPos.dst2(MyScreenAdapter.cam.position) > map.distance * map.distance) {
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
            float dist = pos.dst(cam.position.x, cam.position.y);
            if (closestApproaching != null && closestApproaching.equals(mapable)) {
                focusedDist = dist;
                focusedPos.set(markerX, markerY);
            }
            TextureComponent tex = Mappers.texture.get(mapable);
            if (tex != null) {
                dist -= Math.max(tex.texture.getWidth(), tex.texture.getHeight()) * 0.5f * tex.scale;
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
        if (stationMenu != null) {
            stationMenu.setPosition(Gdx.graphics.getWidth() / 3f, SpaceStationMenu.getHeight(stationMenu), Align.right);
        }
        miniMap.updateMapPosition();
    }
    
    @Override
    public void dispose() {
        font.dispose();
    }

    @Override
    public String toString() {
        infoString.setLength(0);
        infoString.append("[HUD Pool]:         active: ").append(activeNumbers.size)
                .append(", active peak: ").append(activePeak)
                .append(", free: ").append(numbersPool.getFree())
                .append(", peak: ").append(numbersPool.peak)
                .append(", max: ").append(numbersPool.max);
        return infoString.toString();
    }

}
