package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.noise.OpenSimplexNoise;
import com.spaceproject.math.MyMath;

public class NoiseAnim extends TitleAnimation {
    float z;
    float zDelta;
    int size;
    float scale;
    boolean crossSection;
    OpenSimplexNoise noise;
    
    public NoiseAnim(float z, float zDelta, int size, float scale, boolean crossSection) {
        this.z = z;
        this.zDelta = zDelta;
        this.size = size;
        this.scale = scale;
        this.crossSection = crossSection;
        noise = new OpenSimplexNoise(MathUtils.random(Long.MAX_VALUE));
    }
    
    public NoiseAnim() {
        this(0, 0.2f, 10, 0.110f, false);
    }
    
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x <= Gdx.graphics.getWidth() / size; x++) {
            for (int y = 0; y <= Gdx.graphics.getHeight() / size; y++) {
                float e = MyMath.inverseLerp(-1, 1, (float) noise.eval(x * scale, y * scale, z));
                if (crossSection) {
                    if (e > 0.4f && e < 0.5f) {
                        shape.setColor(Color.BLACK);
                        shape.rect(x * size, y * size, size, size);
                    }
                } else {
                    shape.setColor(e, e, e, 1);
                    //if (e > 0.4f && e < 0.5f) shape.setColor(Color.BLACK);
                    shape.rect(x * size, y * size, size, size);
                }
            }
        }
        z += zDelta * delta;
        shape.end();
        
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            scale += 0.001f;
            Gdx.app.log(this.getClass().getSimpleName(), scale + "");
        }
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            scale -= 0.001f;
            Gdx.app.log(this.getClass().getSimpleName(), scale + "");
        }
    }
    
    @Override
    public void resize(int width, int height) {
    }
}
