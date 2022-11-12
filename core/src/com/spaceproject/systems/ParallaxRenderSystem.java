package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ParallaxRenderSystem extends EntitySystem implements Disposable {
    
    private final ShapeRenderer shape;
    private final Matrix4 projectionMatrix;
    private final Vector3 screenCoords = new Vector3();
    private final Vector3 camWorldPos = new Vector3();
    private final Rectangle boundingBox = new Rectangle();
    private ImmutableArray<Entity> players;
    
    public ParallaxRenderSystem() {
        shape = new ShapeRenderer();
        projectionMatrix = new Matrix4();
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
    }
    
    float animate = 0;
    @Override
    public void update(float deltaTime) {
        //update matrix and convert screen coords to world cords.
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        screenCoords.set(0,0,0);
        GameScreen.viewport.project(screenCoords);
        camWorldPos.set(GameScreen.cam.position.cpy());
        GameScreen.viewport.project(camWorldPos);
        boundingBox.set(1, 1, Gdx.graphics.getWidth()-2, Gdx.graphics.getHeight()-2);
    
        //debug override background
        //debugClearScreen();
    
        //render
        shape.begin(ShapeRenderer.ShapeType.Line);
        
        //todo: apply shader to grid
        //drawGrid(Color.GOLD, 100, 1.0f);
        //drawGrid(Color.MAGENTA, 100, 0.5f);
        
        //drawOrigin(Color.SKY);
        //drawCameraPos(Color.RED);
    
        Entity player = players.first();
        Body body = Mappers.physics.get(player).body;
        Vector2 velocity = body.getLinearVelocity();
        Vector2 facing = MyMath.vector(body.getAngle(), 1);
        
        animate += deltaTime;
        //drawEye( 10.0f, boundingBox);
        //drawEye((float) (10.0f + (Math.sin(animate) * 5.0f)), boundingBox);
        //((float) (10.0f + (Math.sin(animate) * 10.0f)), new Rectangle(100F, 200F, 100F, (float) (100 + (Math.sin(animate) * 100.0f))));
        //drawEye((float) (10.0f + ((Math.sin(animate * 10.0f) + MathUtils.PI) * 10.0f)), new Rectangle(100F, 100F, (float) (100 + (Math.sin(animate) * 100.0f)), 100));
        shape.end();
    }
   
    
    private void drawCameraPos(Color color) {
        shape.setColor(color);
        shape.circle(camWorldPos.x, camWorldPos.y, 8);
        shape.line(camWorldPos.x, 0, camWorldPos.x, Gdx.graphics.getHeight());
        shape.line(0, camWorldPos.y, Gdx.graphics.getWidth(), camWorldPos.y);
        
        //todo: draw ring from center to target
        //Vector2 average = getEngine().getSystem(CameraSystem.class).average;
        //Vector2 offsetFromTarget = getEngine().getSystem(CameraSystem.class).offsetFromTarget;
        //shape.circle(camWorldPos.x, camWorldPos.y, offsetFromTarget.len());
        //shape.circle(average.x, average.y, 10);
        //GameScreen.viewport.getWorldHeight();
        /*
        shape.setColor(Color.GREEN);
        shape.circle(GameScreen.viewport.getScreenX(), GameScreen.viewport.getScreenY(), 8);
        shape.line(GameScreen.viewport.getScreenX(), GameScreen.viewport.getScreenY(),
                GameScreen.viewport.getScreenX() + GameScreen.viewport.getWorldWidth(), GameScreen.viewport.getScreenY() + GameScreen.viewport.getWorldHeight());
         */
    }
    
    private void drawOrigin(Color color) {
        shape.setColor(color);
        shape.circle(0, 0, 10);
        shape.circle(screenCoords.x, screenCoords.y, 10);
        shape.line(screenCoords.x, 0, screenCoords.x, Gdx.graphics.getHeight());
        shape.line(0, screenCoords.y, Gdx.graphics.getWidth(), screenCoords.y);
    }
    
    private void drawEye(float segments, Rectangle rectangle) {
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
        shape.setColor(Color.GREEN);
        shape.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.getHeight());
    }
    
    
    private void drawGrid(Color color, int tileSize, float depth) {
        shape.setColor(color);
        
        //unscaled
        for (int horizontal = 0; horizontal <= 10; horizontal++) {
            float offset = horizontal * tileSize;
            //shape.line(screenCoords.x + offset, 0, screenCoords.x + offset, Gdx.graphics.getHeight());
        }
    
        float scale = 1/GameScreen.cam.zoom;
        scale += depth;
        for (int horizontal = 0; horizontal <= 10; horizontal++) {
            float offset = horizontal * tileSize * scale;
            shape.line(screenCoords.x + offset, 0, screenCoords.x + offset, Gdx.graphics.getHeight());
        }
    }
    
    private void debugClearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }
    
}
