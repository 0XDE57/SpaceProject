package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

import static com.spaceproject.screens.MyScreenAdapter.*;


public class GridRenderSystem extends EntitySystem implements Disposable {
    
    //debug options
    public boolean clearScreen = false;
    public boolean drawOrigin = false;
    public boolean drawCameraPos = false;
    public boolean drawCameraPath = false;
    public boolean drawMousePath = false;
    public boolean drawThirdsGrid = false;
    public boolean drawTest = false;
    
    //rendering
    private final ShapeRenderer shape;
    private final Matrix4 projectionMatrix;
    private final Vector3 origin = new Vector3();
    private final Vector3 camWorldPos = new Vector3();
    private final Vector3 mouseProj = new Vector3();
    private final Vector3 playerPos = new Vector3();
    
    private final int gridWidth = 50;
    private Vector3 topLeft = new Vector3();
    private Vector3 bottomRight = new Vector3();
    private final Color gridColor = Color.BLACK.cpy();
    private final Color gridColorB = new Color(.51f, .5f, .5f, 0.3f);
    private final Color ringColor = Color.PURPLE.cpy();
    private final Color lineColor = Color.GREEN.cpy();
    
    private final Color compassColor = Color.WHITE.cpy();
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> orbitEntities;
    private Entity camMarker, mouseMarker;
    private float animate = 0;
    
    private SpriteBatch batch;
    private BitmapFont subFont;
    
    public GridRenderSystem() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        projectionMatrix = new Matrix4();
    
        FreeTypeFontGenerator.FreeTypeFontParameter parameter2 = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter2.size = 10;
        parameter2.borderColor = Color.BLACK;
        parameter2.borderWidth = 3;
        subFont = FontLoader.createFont(FontLoader.fontPressStart, parameter2);
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        orbitEntities = engine.getEntitiesFor(Family.all(OrbitComponent.class).get());
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).exclude(DockedComponent.class).get());
    }
    
    @Override
    public void update(float deltaTime) {
        //warning: system coupling -> todo: use signals?
        HUDSystem hudSystem = getEngine().getSystem(HUDSystem.class);
        if (hudSystem != null && !hudSystem.isDraw()) {
            return;
        }
        
        //update matrix and convert screen coords to world cords.
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(cam.combined);

        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        //render
        shape.setProjectionMatrix(cam.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        drawGrid(GameScreen.isHyper() ? Color.WHITE : gridColor, calculateGridDensity(gridWidth), 2f);
        //drawGrid(GameScreen.isHyper() ? Color.WHITE : gridColor, gridColorB, calculateGridDensity(gridWidth), 2f, 5);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        drawOrbitPath();

        shape.setProjectionMatrix(projectionMatrix);

        //debug override background
        if (clearScreen) debugClearScreen();
        //debug reference points
        if (drawOrigin) drawWorldOrigin(Color.WHITE);
        if (drawCameraPos) drawDebugCameraPos(Color.RED);
        if (drawCameraPath) debugDrawCameraPath(Color.YELLOW);
        if (drawMousePath) debugDrawMousePath();
        if (drawThirdsGrid) drawThirdsGrid(Color.PINK);
        // debug test rendering
        if (drawTest) debugRenderTest(deltaTime);
        
        //draw helpful navigation information
        if (players.size() > 0) {
            drawCompass(players.first());
        }

        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);

        shape.end();
        
        //batch.begin();
        //drawHint("an object in motion, remains in motion");
        //batch.end();
    
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawGrid(Color color, int gridSize, float width) {
        shape.setColor(color);
        //unproject to get edges of viewport
        topLeft.set(0, 0, 0);
        topLeft = viewport.unproject(topLeft);
        bottomRight.set(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0);
        bottomRight = viewport.unproject(bottomRight);
        //draw vertical along X axis
        float thickness = (1/SpaceProject.configManager.getConfig(EngineConfig.class).renderScale) * cam.zoom * width;
        int i = 0;
        int referenceX = (int) (topLeft.x/gridSize);
        int alignedX = referenceX * gridSize;
        for (int x = alignedX; x < bottomRight.x; x += gridSize) {
            float aX = alignedX + (i * gridSize);
            i++;
            shape.rectLine(aX, topLeft.y, aX, bottomRight.y, thickness);
        }
        //draw horizontal along Y axis
        int j = 0;
        int referenceY = (int) (bottomRight.y / gridSize);
        int alignedY = referenceY * gridSize;
        for (int y = alignedY; y < topLeft.y; y += gridSize) {
            float aY = alignedY + (j * gridSize);
            j++;
            shape.rectLine(topLeft.x, aY, bottomRight.x, aY, thickness);
        }
    }

    private void drawGridMultiColor(Color colorA, Color colorB, int gridSize, float width, int emphasisDivisor) {
        //unproject to get edges of viewport
        topLeft.set(0, 0, 0);
        topLeft = viewport.unproject(topLeft);
        bottomRight.set(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0);
        bottomRight = viewport.unproject(bottomRight);
        //draw vertical along X axis
        float thickness = (1/SpaceProject.configManager.getConfig(EngineConfig.class).renderScale) * cam.zoom * width;
        int i = 0;
        int referenceX = (int) (topLeft.x/gridSize);
        int alignedX = referenceX * gridSize;
        for (int x = alignedX; x < bottomRight.x; x += gridSize) {
            float aX = alignedX + (i * gridSize);
            boolean emphasis = (referenceX + i) % emphasisDivisor == 0;
            i++;
            shape.setColor(emphasis ? colorB : colorA);
            shape.rectLine(aX, topLeft.y, aX, bottomRight.y, emphasis ? thickness * 2 : thickness);
        }
        //draw horizontal along Y axis
        int j = 0;
        int referenceY = (int) (bottomRight.y / gridSize);
        int alignedY = referenceY * gridSize;
        for (int y = alignedY; y < topLeft.y; y += gridSize) {
            float aY = alignedY + (j * gridSize);
            boolean emphasis = (referenceY + j) % emphasisDivisor == 0;
            j++;
            shape.setColor(emphasis ? colorB : colorA);
            shape.rectLine(topLeft.x, aY, bottomRight.x, aY, emphasis ? thickness * 2 : thickness);
        }
    }

    private int calculateGridDensity(int width) {
        // calculate "adaptive grid size"
        CameraSystem camera = getEngine().getSystem(CameraSystem.class);
        byte zoomLevel = camera.getZoomLevel();
        byte zoomLevelThreshold = 10; // at what level to start halving grid cells
        for (byte level = zoomLevelThreshold; level <= camera.getMaxZoomLevel(); level++) {
            if (zoomLevel >= level) {
                width *= 2; // double width as we zoom out
            }
        }
        return width;
    }
    
    private void drawOrbitPath() {
        float alpha = MathUtils.clamp((cam.zoom / 150 / 2), 0, 1);
        if (MathUtils.isEqual(alpha, 0)) return;
        
        ringColor.a = alpha;
        lineColor.a = alpha;
        
        for (Entity entity : orbitEntities) {
            OrbitComponent orbit = Mappers.orbit.get(entity);
            TransformComponent entityPos = Mappers.transform.get(entity);
            
            if (orbit.parent != null) {
                TransformComponent parentPos = Mappers.transform.get(orbit.parent);
                shape.setColor(ringColor);
                shape.circle(parentPos.pos.x, parentPos.pos.y, orbit.radialDistance);
                shape.setColor(lineColor);
                shape.line(parentPos.pos.x, parentPos.pos.y, entityPos.pos.x, entityPos.pos.y);
            }
            
            TextureComponent tex = Mappers.texture.get(entity);
            if (tex != null) {
                float radius = tex.texture.getWidth() * 0.5f * tex.scale;
                shape.circle(entityPos.pos.x, entityPos.pos.y, radius);
            }
    
            AsteroidBeltComponent stellarDisk = Mappers.asteroidBelt.get(entity);
            if (stellarDisk != null) {
                Vector2 pos = Mappers.transform.get(entity).pos;
                shape.setColor(lineColor);
                shape.circle(pos.x, pos.y, stellarDisk.radius);
                shape.setColor(ringColor);
                shape.circle(pos.x, pos.y, stellarDisk.radius - (stellarDisk.bandWidth / 2));
                shape.circle(pos.x, pos.y, stellarDisk.radius + (stellarDisk.bandWidth / 2));
            }
        }
    }
    
    private void drawCompass(Entity entity) {
        //set alpha
        float min = 0.05f;
        float maxZoom = CameraSystem.getZoomForLevel(getEngine().getSystem(CameraSystem.class).getMaxZoomLevel());
        float alpha = MathUtils.map(1, maxZoom,min, 1, Math.max(Math.min(GameScreen.cam.zoom*10, maxZoom), 1));
        if (MathUtils.isEqual(alpha, 0)) return;
        
        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
            return;
        }
        
        //draw movement direction for navigation assistance, line up vector with target destination
        Body body = Mappers.physics.get(entity).body;
        Vector2 facing = MyMath.vector(body.getAngle(), 500000);
        Vector2 originalPosition = Mappers.transform.get(entity).pos;
        playerPos.set(originalPosition, 0);
        Vector3 pos = cam.project(playerPos);
        
        float width = 0.5f;
        compassColor.a = alpha;
        shape.rectLine(pos.x, pos.y, facing.x, facing.y, width, compassColor, compassColor);
        Vector2 lead = MyMath.vector(body.getAngle(), 70).add(pos.x, pos.y);
        compassColor.a = 1;
        shape.setColor(compassColor);
        shape.circle(lead.x, lead.y, 2);
    
        //draw movement direction for navigation assistance, line up vector with target destination
        Color compassHighlight = compassColor;
        ControllableComponent control = Mappers.controllable.get(entity);
        if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight) {
            width *= 2;
            compassHighlight = Color.GOLD.cpy();
        }
        if (control.boost) {
            compassHighlight = Color.CYAN.cpy();
        }
        compassHighlight.a = alpha;
        
        //draw velocity vector
        if (body.getLinearVelocity().len() >= 0.1f) {
            Vector2 vel = MyMath.vector(body.getLinearVelocity().angleRad(), 500000).cpy();
            shape.rectLine(pos.x, pos.y, vel.x, vel.y, width, compassHighlight, compassHighlight);
            vel.set(MyMath.vector(body.getLinearVelocity().angleRad(), 50)).add(pos.x, pos.y);
            //draw arrow
            compassHighlight.a = 1;
            shape.setColor(compassHighlight);
            HealthComponent health = Mappers.health.get(entity);
            long hurtTime = 1000;
            if (health != null && (GameScreen.getGameTimeCurrent() - health.lastHitTime < hurtTime)) {
                shape.setColor(Color.RED.cpy());
            }
            int arrowSize = 10;
            Vector2 arrowLeft  = MyMath.vector(body.getLinearVelocity().angleRad() + 135 * MathUtils.degreesToRadians, arrowSize).add(vel);
            shape.rectLine(vel, arrowLeft, 1);
            Vector2 arrowRight = MyMath.vector(body.getLinearVelocity().angleRad() - 135 * MathUtils.degreesToRadians, arrowSize).add(vel);
            shape.rectLine(vel, arrowRight, 1);
        }
    }
    
    private GlyphLayout layout = new GlyphLayout();
    private void drawHint(String text) {
        float ratio = 1 + (float) Math.sin(animate*0.1);
        Color c = Color.GREEN.cpy().lerp(Color.PURPLE, ratio);
        subFont.setColor(c);
        layout.setText(subFont, text);
        
        float centerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        int messageHeight = (int) (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight()/3) + layout.height);
        messageHeight -= layout.height * 2;
        subFont.draw(batch, layout, 0, 0);
    }
    
    //region debug and testing
    private void debugClearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }
    
    private void drawDebugCameraPos(Color color) {
        camWorldPos.set(cam.position);
        GameScreen.viewport.project(camWorldPos);
        shape.setColor(color);
        shape.circle(camWorldPos.x, camWorldPos.y, 8);
        shape.line(camWorldPos.x, 0, camWorldPos.x, Gdx.graphics.getHeight());
        shape.line(0, camWorldPos.y, Gdx.graphics.getWidth(), camWorldPos.y);
    }
    
    private void debugDrawCameraPath(Color color) {
        if (camMarker == null) {
            //add debug camera marker
            TrailComponent spline = new TrailComponent();
            spline.color = color;
            camMarker = new Entity().add(spline).add(new TransformComponent());
            getEngine().addEntity(camMarker);
            Gdx.app.log(this.getClass().getSimpleName(), "debug cam marker activated");
        }
        Mappers.transform.get(camMarker).pos.set(cam.position.x, cam.position.y);
    }
    
    private void drawWorldOrigin(Color color) {
        origin.set(0,0,0);
        GameScreen.viewport.project(origin);
        shape.setColor(color);
        shape.circle(origin.x, origin.y, 10);
        shape.line(origin.x, 0, origin.x, Gdx.graphics.getHeight());
        shape.line(0, origin.y, Gdx.graphics.getWidth(), origin.y);
    }
    
    private void debugDrawMousePath(){
        if (mouseMarker == null) {
            mouseMarker = new Entity().add(new TrailComponent()).add(new TransformComponent());
            mouseMarker.getComponent(TrailComponent.class).color = Color.YELLOW;
            getEngine().addEntity(mouseMarker);
        }
        mouseProj.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        GameScreen.viewport.unproject(mouseProj);
        Mappers.transform.get(mouseMarker).pos.set(mouseProj.x, mouseProj.y);
    }
    
    private void drawThirdsGrid(Color color) {
        float widthThirds = Gdx.graphics.getWidth() / 3.0f;
        float heightThirds = Gdx.graphics.getHeight() / 3.0f;
        shape.setColor(color);
        
        //verticle
        shape.line(widthThirds, 0, widthThirds, Gdx.graphics.getHeight());
        shape.line(widthThirds*2, 0, widthThirds*2, Gdx.graphics.getHeight());
        
        //horizontal
        shape.line(0, heightThirds, Gdx.graphics.getWidth(), heightThirds);
        shape.line(0, heightThirds*2, Gdx.graphics.getWidth(), heightThirds*2);
    }
    
    private void drawEye(float segments, Rectangle rectangle) {
        
        if (segments > 0) {
            shape.setColor(Color.RED);
            float height = rectangle.getHeight() / segments;
            float width = rectangle.getWidth() / segments;
            for (int i = 0; i * height <= rectangle.getHeight(); i++) {
                //bottom right
                shape.line(rectangle.x + i * width,  rectangle.y, rectangle.x + rectangle.getWidth(), rectangle.y + i * height);
                
                //top left
                shape.line(rectangle.x,  rectangle.y + i * height,  rectangle.x + i * width, rectangle.y + rectangle.getHeight());
                
                //bottom left
                //shape.line(rectangle.x, rectangle.y + i * height, rectangle.x + i * width, rectangle.y);
                
                //diagonal
                //shape.line(rectangle.x, rectangle.y  + i * height, rectangle.x + i * width, rectangle.y);
            }
        }
        
        shape.setColor(Color.GREEN);
        shape.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.getHeight());
    }

    private void debugRenderTest(float deltaTime) {
        //drawGrid(Color.WHITE, boundingBox, 50, 3);
        //Color red = Color.RED.cpy();
        //red.a = 0.5f;
        //drawGrid(red, boundingBox, 50, 1);
        Rectangle gridBounds = new Rectangle(1, 1, Gdx.graphics.getWidth()-2, Gdx.graphics.getHeight()-2);
        animate += deltaTime;
        int edgePad = 200;
        Rectangle rectangle = new Rectangle(edgePad, edgePad,
                Gdx.graphics.getWidth() - edgePad * 2,
                Gdx.graphics.getHeight() - edgePad * 2);
        //drawEye((float) (10.0f * Math.sin(animate)), rectangle);
        drawEye((float) (10.0f + (Math.sin(animate) * 5.0f)), gridBounds);
        //((float) (10.0f + (Math.sin(animate) * 10.0f)), new Rectangle(100F, 200F, 100F, (float) (100 + (Math.sin(animate) * 100.0f))));
        //drawEye((float) (10.0f + ((Math.sin(animate * 10.0f) + MathUtils.PI) * 10.0f)), new Rectangle(100F, 100F, (float) (100 + (Math.sin(animate) * 100.0f)), 100));
    }
    //endregion
    
    @Override
    public void dispose() {
        shape.dispose();
        batch.dispose();
    }
    
}
