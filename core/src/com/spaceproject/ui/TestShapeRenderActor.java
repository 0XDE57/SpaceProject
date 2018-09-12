package com.spaceproject.ui;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;


public class TestShapeRenderActor extends Actor {
    private ShapeRenderer shape;

    public TestShapeRenderActor() {
        shape = new ShapeRenderer();

    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        //break out of batch
        //super.draw(batch, parentAlpha);
        batch.end();


        //do custom shape rendering
        shape.setProjectionMatrix(batch.getProjectionMatrix());

        shape.begin(ShapeRenderer.ShapeType.Filled);
        //setBounds(getParent().getX(), getParent().getY(), getParent().getWidth(), getParent().getWidth());


        shape.setColor(Color.GREEN);
        Vector2 coords = new Vector2(getX(), getY());
        localToStageCoordinates(coords);
        shape.circle(coords.x + getWidth()/2, coords.y + getHeight()/2, 10);

        shape.circle(coords.x, coords.y, 10);
        shape.circle(coords.x, coords.y+getHeight(), 10);
        shape.circle(coords.x + getWidth(), coords.y, 10);
        shape.circle(coords.x + getWidth(), coords.y + getHeight(), 10);

        shape.end();
        shape.setColor(Color.WHITE);

        //continue batch
        batch.begin();
    }
}
