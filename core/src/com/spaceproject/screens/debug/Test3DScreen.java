package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.ui.Sprite3D;
import com.spaceproject.utility.MyMath;


public class Test3DScreen extends ScreenAdapter {
    
    
    OrthographicCamera orthographicCam;
    SpriteBatch batch = new SpriteBatch();
    ModelBatch modelBatch = new ModelBatch();
    Environment environment;
    
    EntityConfig entityCFG;
    Sprite3D ship3d;
    
    int playerX, playerY;
    float rotX = 0, rotY = 0, rotZ = 0;
    
    public Test3DScreen() {
        entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
        orthographicCam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1.f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -.4f, -.4f, -.4f));
        
        generateShip();
    }
    
    private void generateShip() {
        long seed = MathUtils.random(Long.MAX_VALUE);
        Texture body = TextureFactory.generateShip(seed, MathUtils.random(20, 30));
        Texture wing = TextureFactory.generateShipWingLeft(seed, (body.getWidth() + 1) / 2);
        Texture shipTop = TextureFactory.combineShip(body, wing);
        Texture shipBottom = TextureFactory.generateShipUnderSide(shipTop);
        
        ship3d = new Sprite3D(shipTop, shipBottom, entityCFG.renderScale);
    }
    
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            generateShip();
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            playerX -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            playerX += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            playerY += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            playerY -= 1f;
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            rotX += 400f * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            rotX -= 400f * delta;
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            orthographicCam.zoom += 1 * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            orthographicCam.zoom -= 1 * delta;
        }
        
        orthographicCam.position.x = playerX;
        orthographicCam.position.y = playerY;
        
        
        orthographicCam.update();
        batch.setProjectionMatrix(orthographicCam.combined);
        /*
        batch.begin();
        batch.draw(combinedTex, 100,100,50,50);


        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight()-Gdx.input.getY();
        draw(combinedTex, x, y);
        draw(shipTop, x + (int)(combinedTex.getWidth()*SpaceProject.entitycfg.renderScale) + 10, y + 25);
        draw(shipBottom, x + (int)(combinedTex.getWidth()*SpaceProject.entitycfg.renderScale) + 10, y - 25);

        batch.end();
        */
        
        modelBatch.begin(orthographicCam);
        modelBatch.render(ship3d);//, environment);
        modelBatch.end();
        
        
        //ship3d.worldTransform.rotate(Vector3.X, 90 * delta);
        //ship3d.worldTransform.rotate(Vector3.Y, 60 * delta);
        //ship3d.worldTransform.rotate(Vector3.Z, 90 * delta);
        ship3d.worldTransform.setToRotation(Vector3.Z, MyMath.angleTo(
                (int) Gdx.graphics.getWidth() / 2,//ship3d.worldTransform.getTranslation(Vector3.X).x,
                (int) Gdx.graphics.getHeight() / 2,//ship3d.worldTransform.getTranslation(Vector3.Y).y,
                Gdx.input.getX(),
                Gdx.graphics.getHeight() - Gdx.input.getY()) * MathUtils.radDeg);
        
        
        ship3d.worldTransform.rotate(Vector3.X, rotX);
        ship3d.worldTransform.setTranslation(playerX, playerY, -50);//bring z closer to camera so it doesn't clip outside the camera's near & far (should be at least sprites width/height)
        
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
    }
    
    private void draw(Texture tex, int x, int y) {
        float width = tex.getWidth();
        float height = tex.getHeight();
        float originX = width * 0.5f; //center
        float originY = height * 0.5f; //center
        float scale = entityCFG.renderScale;
        
        //draw texture
        batch.draw(tex, (x - originX), (y - originY),
                originX, originY,
                width, height,
                scale, scale,
                0,
                0, 0, (int) width, (int) height, false, false);
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }
    
    @Override
    public void show() {
        super.show();
    }
    
    @Override
    public void hide() {
        super.hide();
    }
    
    @Override
    public void pause() {
        super.pause();
    }
    
    @Override
    public void resume() {
        super.resume();
    }
    
    @Override
    public void dispose() {
        modelBatch.dispose();
        batch.dispose();
    }
}



