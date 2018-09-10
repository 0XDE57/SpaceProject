package com.spaceproject.ui;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class TestShapeRenderActor extends Actor {
    private ShapeRenderer shape;

    public TestShapeRenderActor() {
        shape = new ShapeRenderer();

    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        //break out of batch
        super.draw(batch, parentAlpha);
        batch.end();

        //do custom shape rendering
        shape.setProjectionMatrix(batch.getProjectionMatrix());

        shape.begin(ShapeRenderer.ShapeType.Filled);

        shape.setColor(Color.GREEN);
        shape.circle(getX() + getWidth()/2, getY()+ getHeight()/2, 10);
        shape.end();
        shape.setColor(Color.WHITE);

        //continue batch
        batch.begin();
    }
}
