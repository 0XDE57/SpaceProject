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
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Null;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.config.UIConfig;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.map.MapState;
import com.spaceproject.ui.map.MiniMap;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.IScreenResizeListener;
import com.spaceproject.utility.Mappers;


public class HUDSystem extends EntitySystem implements IRequireGameContext, IScreenResizeListener, Disposable {
    
    private final UIConfig uiCFG = SpaceProject.configManager.getConfig(UIConfig.class);
    private final KeyConfig keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
    
    private GameMenu gameMenu;
    
    //rendering
    private final OrthographicCamera cam;
    private final Matrix4 projectionMatrix;
    private final ShapeRenderer shape;
    private final SpriteBatch batch;
    private final BitmapFont font, subFont, inventoryFont;
    private final GlyphLayout layout = new GlyphLayout();
    
    //entity storage
    private ImmutableArray<Entity> mapableEntities;
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> killableEntities;
    
    private MiniMap miniMap;
    
    private boolean drawHud = true;
    private boolean drawEdgeMap = true;
    
    enum SpecialState {
        off, docked, hyper, landing, launching, destroyed;
    }

    float anim = 0;
    
    public HUDSystem() {
        cam = MyScreenAdapter.cam;
        shape = MyScreenAdapter.shape;
        batch = MyScreenAdapter.batch;
        projectionMatrix = new Matrix4();
    
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
        killableEntities = engine.getEntitiesFor(Family.all(HealthComponent.class, TransformComponent.class).exclude(
                ControlFocusComponent.class, AsteroidComponent.class).get());
    }
    
    @Override
    public void update(float delta) {
        checkInput();
    
        if (drawHud) {
            anim += 4f * delta;
            drawHUD();
        
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
    
    private void drawHUD() {
        //set projection matrix so things render using correct coordinates
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(projectionMatrix);
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shape.begin(ShapeType.Filled);
    
        Entity player = null;
        if (players.size() > 0) {
            player = players.first();
        }
        drawPlayerStatus(player);
        
        if (drawEdgeMap) {
            drawEdgeMap();
        }
        
        drawHealthBars();
    
        /*
        shape.setColor(Color.BLACK);
        float padding = 10;
        //shape.rectLine(centerX-padding, messageHeight-((layout.height-padding)*0.35f),
        //        centerX+layout.width+(padding*2), messageHeight-((layout.height-padding)*0.35f), layout.height + (padding*2));
        //shape.rectLine(0, messageHeight, , messageHeight, Gdx.graphics.getWidth(),layout.height);*/
        
        shape.end();
        
        batch.begin();

        CameraSystem camSystem = getEngine().getSystem(CameraSystem.class);
        if (!GameScreen.isHyper() && camSystem.getZoomLevel() != camSystem.getMaxZoomLevel()) {
            drawInventory(player, 100, 100);
        }
        
        //draw special state: hyper or landing / launching
        drawSpecialStateMessage(player);

        if (player == null) {
            ImmutableArray<Entity> respawnEntities = getEngine().getEntitiesFor(Family.all(RespawnComponent.class).get());
            if (respawnEntities.size() > 0) {
                RespawnComponent respawn = Mappers.respawn.get(respawnEntities.first());
                drawHint(respawn.reason);
            }
        } else {
            //if player is over planet hint
            if (GameScreen.inSpace()) {
                ControllableComponent control = Mappers.controllable.get(player);
                if (control != null && control.canTransition && (Mappers.screenTrans.get(player) == null)) {
                    String input = getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus() ? "D-Pad Down" : "T";
                    drawHint("press [" + input + "] to land");
                }
            }
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
    
    private void checkInput() {
        //todo: move to desktop input
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleHUD)) {
            drawHud = !drawHud;
            Gdx.app.log(getClass().getSimpleName(), "HUD: " + drawHud);
        }
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleEdgeMap)) {
            drawEdgeMap = !drawEdgeMap;
            Gdx.app.log(getClass().getSimpleName(), "Edge mapState: " + drawEdgeMap);
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
            HealthComponent health = Mappers.health.get(entity);
            //ignore full health
            if (!uiCFG.renderFullHealth && health.health == health.maxHealth) {
                continue;
            }
            
            Vector3 pos = cam.project(new Vector3(Mappers.transform.get(entity).pos.cpy(), 0));
            
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
    private void drawSpecialStateMessage(Entity player) {
        SpecialState messageState = SpecialState.off;
        if (player == null) {
            messageState = SpecialState.destroyed;
        } else {
            HyperDriveComponent hyper = Mappers.hyper.get(player);
            if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                messageState = SpecialState.hyper;
            }
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
        
        float ratio = 1 + (float) Math.sin(anim);
        Color c = Color.GOLD.cpy().lerp(Color.CYAN, ratio);
        switch (messageState) {
            case docked:
                String input = getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus() ? "D-Pad ???" : "E";
                drawHint("press [" + input + "] to interact");
                layout.setText(font, "[ DOCKED ]");
            break;
            case hyper: layout.setText(font, "[ HYPERDRIVE ]"); break;
            case landing: layout.setText(font, "[ LANDING ]"); break;
            case launching: layout.setText(font, "[ LAUNCHING ]"); break;
            case destroyed:
                layout.setText(font, "[ DESTROYED ]");
                c = Color.BLACK.cpy().lerp(Color.RED, ratio);
                break;
            case off:
                return;
        }
        font.setColor(c);
        
        float centerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        int messageHeight = (int) (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight()/3) + layout.height);
        font.draw(batch, layout, centerX, messageHeight);
    }
    
    private void drawHint(String text) {
        float ratio = 1 + (float) Math.sin(anim*0.1);
        Color c = Color.GREEN.cpy().lerp(Color.PURPLE, ratio);
        subFont.setColor(c);
        layout.setText(subFont, text);
        
        float centerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        int messageHeight = (int) (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight()/3) + layout.height);
        messageHeight -= layout.height * 2;
        subFont.draw(batch, layout, centerX, messageHeight);
    }
    
    private void drawPlayerStatus(Entity entity) {
        if (entity == null) return;
        
        int barWidth = uiCFG.playerHPBarWidth;
        int barHeight = uiCFG.playerHPBarHeight;
        int barX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int healthBarY = uiCFG.playerHPBarY;
        int ammoBarY = healthBarY - barHeight - 1;
        int hyperBarY = healthBarY + barHeight + 1;
        
        //todo: force certain elements when hud is off (bypass)
        // 1. [x] hyperdrive should bring up velocity
        // 2. [ ] health when take damage (timer)
        // 3. [ ] shield when broken? (broken state not implemented yet)
        
        drawPlayerVelocity(entity, barX, hyperBarY, barWidth, barHeight);
        drawHyperDriveBar(entity, barX, hyperBarY, barWidth, barHeight);
    
        CameraSystem cam = getEngine().getSystem(CameraSystem.class);
        if (cam.getZoomLevel() == cam.getMaxZoomLevel()) return;
        
        if (GameScreen.isHyper()) return;
        
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
        shape.setColor(1 - ratioHP, ratioHP, 0, uiCFG.entityHPbarOpacity);
        if (GameScreen.getGameTimeCurrent() - health.lastHitTime < 1000) {
            shape.setColor(1, 0, 0, 1);

            int thickness = 2;
            shape.rectLine(x, y+height, x+width, y+height, thickness);//top
            shape.rectLine(x, y, x+width, y,thickness);//bottom
            shape.rectLine(x, y+height, x, y, thickness);//left
            shape.rectLine(x+width, y+height, x+width, y, thickness);//right
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
            shape.setColor(Color.GOLD);
            if (control.boost) {
                shape.setColor(Color.CYAN);
            }
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
    
    private void drawInventory(Entity entity, float x, float y) {
        if (entity == null) return;
        CargoComponent cargo = Mappers.cargo.get(entity);
        if (cargo == null) return;

        int barWidth = uiCFG.playerHPBarWidth;
        int barHeight = uiCFG.playerHPBarHeight;
        int barX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int healthBarY = uiCFG.playerHPBarY;

        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            ControllableComponent control = Mappers.controllable.get(entity);
            if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight || control.boost) {
                inventoryFont.setColor(Color.GOLD);
                if (control.boost) {
                    inventoryFont.setColor(Color.CYAN);
                }
            }
            inventoryFont.draw(batch, " " + MyMath.round(physics.body.getLinearVelocity().len(), 1), barX + barWidth, healthBarY + barHeight + layout.height);
        }

        long colorTime = 1000;
        long timeSinceCollect = GameScreen.getGameTimeCurrent() - cargo.lastCollectTime;
        float ratio = (float) timeSinceCollect / (float) colorTime;
        inventoryFont.setColor(ratio, 1, ratio, 1);
        layout.setText(inventoryFont, " Inv: " + cargo.count);
        inventoryFont.draw(batch, layout, barX + barWidth, healthBarY + layout.height);

        inventoryFont.setColor(1, 1, 1, 1);
        inventoryFont.draw(batch, " Crd: " + cargo.credits, barX + barWidth, healthBarY - layout.height);
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
        
        //todo: add navigation point for current direction facing
        //todo: add navigation point for current velocity vector
        boolean drawBorder = true;
        if (drawBorder) {
            //todo: look broken dont see border
            shape.setColor(Color.RED);
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
    
    @Override
    public void dispose() {
        font.dispose();
    }
}
