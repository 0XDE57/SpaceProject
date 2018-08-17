package com.spaceproject.screens.menuanim;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.OpenSimplexNoise;

public class NoiseAnim extends MainMenuAnimation {
    float z;
    float zDelta;
    int size;
    float scale;
    OpenSimplexNoise noise;

    public NoiseAnim() {
        z = 0;
        zDelta = 0.2f;
        size = 10;
        scale = 0.110f;
        noise = new OpenSimplexNoise();
    }

    @Override
    public void render(float delta, ShapeRenderer shape) {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < Gdx.graphics.getWidth()/size; x++) {
            for (int y = 0; y < Gdx.graphics.getHeight()/size; y++) {
                float e = MyMath.inverseLerp(-1,1, (float)noise.eval(x*scale, y*scale, z));
                shape.setColor(e,e,e,1);
                shape.rect(x*size, y*size, size, size);
            }
        }
        z+= zDelta * delta;
        shape.end();

        if (Gdx.input.isKeyPressed(Input.Keys.V)) scale += 0.001f;
        if (Gdx.input.isKeyPressed(Input.Keys.B)) scale -= 0.001f;
    }
}
