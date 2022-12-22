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
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CannonComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.config.UIConfig;
import com.spaceproject.generation.FontFactory;
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
    private OrthographicCamera cam;
    private Matrix4 projectionMatrix;
    private ShapeRenderer shape;
    private SpriteBatch batch;
    private BitmapFont font, subFont;
    private GlyphLayout layout = new GlyphLayout();
    
    //entity storage
    private ImmutableArray<Entity> mapableEntities;
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> killableEntities;
    
    private MiniMap miniMap;
    
    private boolean drawHud = true;
    private boolean drawEdgeMap = true;
    
    enum SpecialState {
        off, hyper, landing, launching;
    }
    SpecialState messageState = SpecialState.off;
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
        font = FontFactory.createFont(FontFactory.fontPressStart, parameter);
    
        FreeTypeFontGenerator.FreeTypeFontParameter parameter2 = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter2.size = 20;
        parameter2.borderColor = Color.BLACK;
        parameter2.borderWidth = 3;
        subFont = FontFactory.createFont(FontFactory.fontPressStart, parameter2);
        
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
        
        Entity player = players.first();
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
        
        //draw special state: hyper or landing / launching
        drawSpecialStateMessage(player);
        //todo: if player is over planet
        //drawHint("press [T] to land");
        //drawHint("stars are hot");
        //drawHint("an object in motion remains in motion");
    
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    private void checkInput() {
        //todo: move to desktop input
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
        if (player == null) return;
        
        messageState = SpecialState.off;
        
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
    
        float ratio = 1 + (float) Math.sin(anim);
        Color c = Color.GOLD.cpy().lerp(Color.CYAN, ratio);
        font.setColor(c);
        switch (messageState) {
            case hyper: layout.setText(font, "[ HYPERDRIVE ]"); break;
            case landing: layout.setText(font, "[ LANDING ]"); break;
            case launching: layout.setText(font, "[ LAUNCHING ]"); break;
            case off:
                return;
        }
        
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
        
        CameraSystem cam = getEngine().getSystem(CameraSystem.class);
        if (cam.getZoomLevel() == cam.getMaxZoomLevel()) {
            return;
        }
    
        drawPlayerVelocity(entity, barX, hyperBarY, barWidth, barHeight);
        drawHyperDriveBar(entity, barX, hyperBarY, barWidth, barHeight);
    
        if (GameScreen.isHyper()) return;
        
        drawPlayerHealth(entity, barX, healthBarY, barWidth, barHeight);
        drawPlayerShield(entity, barX, healthBarY, barWidth, barHeight);
        drawPlayerAmmoBar(entity, barX, ammoBarY, barWidth, barHeight);

		/*
		//border
		//shape.setColor(new Color(0.1f, 0.63f, 0.88f, 1f));
        shape.setColor(Color.DARK_GRAY);
		int thickness = 2;
		shape.rectLine(barX, healthBarY+barHeight*2, barX+barWidth, healthBarY+barHeight*2, thickness);//top
		shape.rectLine(barX, healthBarY-barHeight, barX+barWidth, healthBarY-barHeight,thickness);//bottom
		shape.rectLine(barX, healthBarY+barHeight, barX, healthBarY-barHeight, thickness);//left
		shape.rectLine(barX+barWidth, healthBarY+barHeight, barX+barWidth, healthBarY-barHeight, thickness);//right
		*/
    }
    
    private void drawPlayerHealth(Entity entity, int x, int y, int width, int height) {
        HealthComponent health = Mappers.health.get(entity);
        if (health != null) {
            shape.setColor(uiCFG.entityHPbarBackground);
            shape.rect(x, y, width, height);
    
            float ratioHP = health.health / health.maxHealth;
            shape.setColor(1 - ratioHP, ratioHP, 0, uiCFG.entityHPbarOpacity);
            shape.rect(x, y, width * ratioHP, height);
        }
    }
    
    private void drawPlayerShield(Entity entity, int x, int y, int width, int height) {
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield == null || shield.state == ShieldComponent.State.off) {
            return;
        }
    
        //if shield not engaged, render bar half width
        if (shield.state != ShieldComponent.State.on) {
            int halfHeight = height / 2;
            height = halfHeight;
            y += halfHeight / 2;
        }
    
        float ratioShield = shield.radius / shield.maxRadius;
        if (shield.state == ShieldComponent.State.on) {
            shape.setColor(shield.color);
        } else {
            shape.setColor(shield.color.r, shield.color.g, shield.color.b, 0.25f);
        }
        shape.rect(x, y, width * ratioShield, height);
    }
    
    private void drawPlayerAmmoBar(Entity entity, int x, int y, int width, int height) {
        drawCannonAmmoBar(entity, x, y, width, height);
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
    
    private void drawCannonAmmoBar(Entity entity, int x, int y, int width, int height) {
        CannonComponent cannon = Mappers.cannon.get(entity);
        if (cannon == null) return;
        
        float ratioAmmo = (float) cannon.curAmmo / (float) cannon.maxAmmo;
        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rect(x, y, width, height);
        shape.setColor(uiCFG.playerAmmoBarColor);
        shape.rect(x, y, width * ratioAmmo, height);
        
        for (int i = 0; i < cannon.maxAmmo; i++) {
            int xDiv = x + (i * width / cannon.maxAmmo);
            //draw recharge bar
            if (i == cannon.curAmmo) {
                shape.setColor(uiCFG.playerAmmoBarRechargeColor);
                shape.rect(xDiv, y, width / cannon.maxAmmo * cannon.timerRechargeRate.ratio(), height);
            }
            //draw divisions to mark individual ammo
            if (i > 0) {
                shape.setColor(Color.BLACK);
                shape.rectLine(xDiv, y + height, xDiv, y, 3);
            }
        }
        
		/*
		//draw recharge bar (style 2)
		if (cannon.curAmmo < cannon.maxAmmo) {
			int rechargeBarHeight = 2;
			shape.setColor(Color.SLATE);
			shape.rect(x, playerBarY, width * cannon.timerRechargeRate.ratio(), rechargeBarHeight);
		}
		*/
    
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
