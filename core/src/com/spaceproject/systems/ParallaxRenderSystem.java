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
import com.spaceproject.components.AsteroidBeltComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.SplineComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

import static com.spaceproject.screens.MyScreenAdapter.cam;

//todo: rename? this is more of a grid render system / "under hud" frame of reference tool
public class ParallaxRenderSystem extends EntitySystem implements Disposable {
    
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
    
    private final int gridWidth = 400;
    private final Rectangle gridBounds = new Rectangle();
    private final Color gridColor = Color.BLACK.cpy();
    private final Color ringColor = Color.PURPLE.cpy();
    private final Color lineColor = Color.GREEN.cpy();
    
    private final Color compassColor = Color.WHITE.cpy();
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> orbitEntities;
    private Entity camMarker, mouseMarker;
    private float animate = 0;
    
    private SpriteBatch batch;
    private BitmapFont subFont;
    
    public ParallaxRenderSystem() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        projectionMatrix = new Matrix4();
    
        FreeTypeFontGenerator.FreeTypeFontParameter parameter2 = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter2.size = 10;
        parameter2.borderColor = Color.BLACK;
        parameter2.borderWidth = 3;
        subFont = FontFactory.createFont(FontFactory.fontPressStart, parameter2);
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        orbitEntities = engine.getEntitiesFor(Family.all(OrbitComponent.class).get());
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
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
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(cam.combined);
        
        origin.set(0,0,0);
        GameScreen.viewport.project(origin);
        camWorldPos.set(cam.position);
        GameScreen.viewport.project(camWorldPos);
        gridBounds.set(1, 1, Gdx.graphics.getWidth()-2, Gdx.graphics.getHeight()-2);
    
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        //render
        shape.begin(ShapeRenderer.ShapeType.Line);
        
        //draw grid
        gridColor.a = 0.15f;
        drawGrid(gridColor, gridBounds, gridWidth, 0.5f);
        
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
        
        shape.setProjectionMatrix(cam.combined);
        //if (!GameScreen.isHyper())
        drawOrbitPath();
        
        
        shape.end();
        
        //batch.begin();
        //drawHint("an object in motion, remains in motion");
        //batch.end();
    
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    private void drawGrid(Color color, Rectangle rect, int gridSize, float width) {
        shape.setColor(color);
        
        float halfWidth = rect.width * 0.5f;
        float halfHeight = rect.height * 0.5f;
        float centerX = rect.x + halfWidth;
        float centerY = rect.y + halfHeight;
        float scale = cam.zoom;
        float relativeGridWidth = gridSize / scale;
        
        //dynamic size
        boolean adaptiveGrid = false;
        if (adaptiveGrid) {
            if (relativeGridWidth < gridSize) {
                gridSize *= 2.0f;
            }
            relativeGridWidth = gridSize / scale;
            /*
            if (relativeGridWidth < gridSize * 2f) {
                gridSize *= 0.5f;
                relativeGridWidth = gridSize / scale;
            }*/
        }
        int countX = 0, countY = 0;
        
        //draw X: horizontal lines
        float posX = cam.position.x;
        int startX = (int) (posX + (-halfWidth * scale)) / gridSize;
        int endX = (int) (posX + (halfWidth * scale)) / gridSize;
        for (int i = startX; i < endX + 1; i++) {
            float finalX = (((i * gridSize) - posX) / scale) + centerX;
            if (width > 1) {
                finalX -= width * 0.5f;
            }
            countX++;
            shape.rect(finalX, rect.y, width, rect.height);
        }
        
        //gridSize *= 1.0f / (16.0f / 9.0f);//test 16x9 asymmetrical grid
        
        //draw Y: vertical lines
        float posY = cam.position.y;
        int startY = (int) (posY + (-halfHeight * scale)) / gridSize;
        int endY = (int) (posY + (halfHeight * scale)) / gridSize;
        for (int i = startY; i < endY + 1; i++) {
            float finalY = (((i * gridSize) - posY) /  scale) + centerY;
            if (width > 1) {
                finalY -= width * 0.5f;
            }
            countY++;
            shape.rect(rect.x, finalY, rect.width, width);
        }
        
        //todo: highlight tile
        //draw grid origin
        //camera (center tile)
        //mouse
        //draw grid co'ods
        //
        
        int tilesX = countX + 1;
        int tilesY = countY + 1;
        
        boolean showDebug = false;
        if (showDebug) {
            DebugSystem.addDebugText(countX + ", " + countY
                    + " | " + tilesX + ", " + tilesY
                    + " | " + relativeGridWidth, rect.x + rect.width/2, rect.y + 20);
            
            //border
            shape.setColor(new Color(0.1f, 0.63f, 0.88f, 1f));
            shape.rect(rect.x, rect.y, rect.width, rect.height);
    
            //center
            shape.setColor(Color.PURPLE);
            shape.circle(centerX, centerY, 10);
            shape.line(centerX, rect.x, centerX, rect.y + rect.height);
            shape.line(rect.x, centerY, rect.x + rect.width, centerY);
        }
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
        float alpha = MathUtils.clamp((cam.zoom / 150 / 2), 0, 1);
        if (MathUtils.isEqual(alpha, 0)) return;
        
        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
            return;
        }
        
        //draw movement direction for navigation assistance, line up vector with target destination
        Body body = Mappers.physics.get(entity).body;
        Vector2 facing = MyMath.vector(body.getAngle(), 50000);
        Vector2 originalPosition = Mappers.transform.get(entity).pos;
        playerPos.set(originalPosition, 0);
        Vector3 pos = cam.project(playerPos);
        
        float width = 0.5f;
        compassColor.a = alpha;
        shape.rectLine(pos.x, pos.y, facing.x, facing.y, width, compassColor, compassColor);
    
        //draw movement direction for navigation assistance, line up vector with target destination
        Color compassHighlight = compassColor;
        ControllableComponent control = Mappers.controllable.get(entity);
        if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight) {
            width *= 2;
            compassHighlight = Color.GOLD.cpy();
            if (control.boost) {
                compassHighlight = Color.CYAN.cpy();
            }
        }
        compassHighlight.a = alpha;
        
        //draw velocity vector
        float vel2 = body.getLinearVelocity().len2();
        if (vel2 > 1) {
            Vector2 vel = MyMath.vector(body.getLinearVelocity().angleRad(), 50000);
            shape.rectLine(pos.x, pos.y, vel.x, vel.y, width, compassHighlight, compassHighlight);
        }
        
        //draw circle; radius = velocity
        //shape.setProjectionMatrix(GameScreen.cam.combined);
        //yellow when engine engaged cyan when boost engaged
        float relVel = vel2 / Box2DPhysicsSystem.getVelocityLimit2();
        float radius = 2 + 2 * relVel;
    
        //compassHighlight.a = 1;
        //shape.setColor(compassHighlight);
        //shape.line(originalPosition.x, originalPosition.y,);
        //shape.circle(originalPosition.x, originalPosition.y, radius);
        //shape.circle(originalPosition.x, originalPosition.y, 2);
        
        //draw engine impulses
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
        shape.setColor(color);
        shape.circle(camWorldPos.x, camWorldPos.y, 8);
        shape.line(camWorldPos.x, 0, camWorldPos.x, Gdx.graphics.getHeight());
        shape.line(0, camWorldPos.y, Gdx.graphics.getWidth(), camWorldPos.y);
    }
    
    private void debugDrawCameraPath(Color color) {
        if (camMarker == null) {
            //add debug camera marker
            SplineComponent spline = new SplineComponent();
            spline.color = color;
            camMarker = new Entity().add(spline).add(new TransformComponent());
            getEngine().addEntity(camMarker);
            Gdx.app.log(this.getClass().getSimpleName(), "debug cam marker activated");
        }
        Mappers.transform.get(camMarker).pos.set(cam.position.x, cam.position.y);
    }
    
    private void drawWorldOrigin(Color color) {
        shape.setColor(color);
        shape.circle(origin.x, origin.y, 10);
        shape.line(origin.x, 0, origin.x, Gdx.graphics.getHeight());
        shape.line(0, origin.y, Gdx.graphics.getWidth(), origin.y);
        
        //shape.rect(screenCoords.x, rect.y, width, rect.height);
        //shape.rect(0, screenCoords.y, rect.width, width);
    }
    
    private void debugDrawMousePath(){
        if (mouseMarker == null) {
            mouseMarker = new Entity().add(new SplineComponent()).add(new TransformComponent());
            mouseMarker.getComponent(SplineComponent.class).color = Color.YELLOW;
            getEngine().addEntity(mouseMarker);
        }
        mouseProj.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        GameScreen.viewport.unproject(mouseProj);
        Mappers.transform.get(mouseMarker).pos.set(mouseProj.x, mouseProj.y);
    }
    
    private void drawThirdsGrid(Color color) {
        float widthThirds = Gdx.graphics.getWidth()/3;
        float heightThirds = Gdx.graphics.getHeight()/3;
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
