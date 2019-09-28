package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.generation.TextureFactory;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;

import java.util.ArrayList;

public class TestShipGenerationScreen extends ScreenAdapter {
    
    
    SpriteBatch batch = new SpriteBatch();
    
    public ArrayList<Texture> ships;
    
    int numShips = 32;
    int scale = 4;
    int rows = 8;
    int spacing = 150;
    
    public TestShipGenerationScreen() {
        
        ships = generateShips();
        
    }
    
    
    private ArrayList<Texture> generateShips() {
        ArrayList<Texture> tex = new ArrayList<Texture>();
        MathUtils.random.setSeed(GameScreen.SEED);
        
        while (tex.size() < numShips) {
            Texture body = TextureFactory.generateShip(MathUtils.random(Long.MAX_VALUE), MathUtils.random(10, 36));
            Texture leftWing = TextureFactory.generateShipWingLeft(MathUtils.random(Long.MAX_VALUE), (body.getWidth() + 1) / 2);
            Texture rightWing = TextureFactory.FlipTexture(leftWing, false, true);
            tex.add(leftWing);
            tex.add(rightWing);
            tex.add(body);
            tex.add(TextureFactory.combineShip(body, leftWing));
        }
        
        
        return tex;
    }
    
    public void render(float delta) {
        Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        int hor = Gdx.graphics.getWidth() / 2;
        int ver = Gdx.graphics.getHeight() / 2;
        
        
        batch.begin();
        for (int i = 0; i < ships.size(); i++) {
            int x = i % rows;
            int y = i / rows;
            
            Texture tex = ships.get(i);
            float width = tex.getWidth();
            float height = tex.getHeight();
            float originX = width * 0.5f; //center
            float originY = height * 0.5f; //center
            
            //draw texture
            batch.draw(tex, x * spacing + (hor / 6), y * spacing + (ver / 2),
                    originX, originY,
                    width, height,
                    scale, scale,
                    (float) Math.PI / 2 * MathUtils.radiansToDegrees,
                    0, 0, (int) width, (int) height, false, false);
        }
        batch.end();
        
        
        if (Gdx.input.isKeyJustPressed(Keys.R)) {
            //generate new set of ships
            GameScreen.SEED = MathUtils.random(Long.MAX_VALUE);
            ships.clear();
            ships = generateShips();
        }
        
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
        
    }
    
    //resize game
    public void resize(int width, int height) {
        Gdx.app.log("screen", width + ", " + height);
    }
    
    public void dispose() {
    }
    
    public void hide() {
    }
    
    public void pause() {
    }
    
    public void resume() {
    }
    
}
