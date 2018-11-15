package com.spaceproject.ui;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.systems.SpaceLoadingSystem;
import com.spaceproject.utility.SimpleTimer;

public class MiniMap {

    public MapState mapState = MapState.off;
    private MiniMapPosition miniMapPosition = MiniMapPosition.bottomRight;

    private int chunkSize;
    private int borderWidth = 3;
    private int size = 6;
    private float mapScale;

    private SimpleTimer drawScaleTimer = new SimpleTimer(5000);

    private Rectangle mapBacking;

    private BitmapFont fontSmall;

    public MiniMap() {
        updateMapPosition();
        resetMapScale();

        chunkSize = SpaceProject.uicfg.mapChunkSize;

        fontSmall = FontFactory.createFont(FontFactory.fontPressStart, 12);
    }

    public void drawSpaceMap(Engine engine, ShapeRenderer shape, SpriteBatch batch) {
        if (mapState == MapState.off)
            return;

        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);

        if (mapScale < 1)
            mapScale = 1;

        //updateMapPosition();
        float centerMapX = mapBacking.x + mapBacking.width / 2;
        float centerMapY = mapBacking.y + mapBacking.height / 2;


        //draw backing
        shape.setColor(0, 0, 0, 0.8f);
        shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, mapBacking.height);


        //draw mouse pos
        shape.setColor(1f, 0.2f, 0.2f, 1f);
        int mX = Gdx.input.getX();
        int mY = Gdx.graphics.getHeight()-Gdx.input.getY();
        if (mapBacking.contains(mX, mY)) {
            shape.line(mapBacking.x, mY, mapBacking.x+mapBacking.width, mY);//horizontal
            shape.line(mX, mapBacking.y, mX, mapBacking.y+mapBacking.height);//vertical
        }


        //draw grid X
        shape.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        //int scaledChunk = (int)(chunkSize / mapScale);
        //int chunkPosX = (int)(MyScreenAdapter.cam.position.x / mapScale);
        int halfWidth = (int)(((mapBacking.width / 2)));
        int startX = (int)((-halfWidth * mapScale) + MyScreenAdapter.cam.position.x)/chunkSize;
        int endX = (int)((halfWidth * mapScale) + MyScreenAdapter.cam.position.x)/chunkSize;
        //TODO: when lines render outside the box
        //when scale = 500 and x > ~80,000

        //System.out.println(startX + " , " +endX);
        for (int i = startX; i < endX+1; i++) {
            float finalX = (((i*chunkSize) - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
            shape.rect(finalX, mapBacking.y, 1, mapBacking.height);
        }


        // draw grid Y
        int halfHeight = (int)(((mapBacking.height / 2)));
        int startY = (int)((-halfHeight * mapScale) + MyScreenAdapter.cam.position.y) / chunkSize;
        int endY = (int)((halfHeight * mapScale) + MyScreenAdapter.cam.position.y) / chunkSize;
        for (int i = startY; i < endY+1; i++) {
            float finalY = (((i*chunkSize) - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
            shape.rect(mapBacking.x, finalY, mapBacking.width, 1);
        }



        //draw mapState objects
        shape.setColor(1, 1, 0, 1);
        SpaceLoadingSystem spaceLoader = engine.getSystem(SpaceLoadingSystem.class);
        if (spaceLoader != null) {
            for (Vector2 p : spaceLoader.getPoints()) {
                // n = relative pos / scale + mapPos
                float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

                if (mapBacking.contains(x, y)) {
                    shape.circle(x, y, size);
                }
            }
        }


        //draw border
        shape.setColor(0.6f,0.6f,0.6f,1f);
        shape.rect(mapBacking.x, mapBacking.height+mapBacking.y-borderWidth, mapBacking.width, borderWidth);//top
        shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, borderWidth);//bottom
        shape.rect(mapBacking.x, mapBacking.y, borderWidth, mapBacking.height);//left
        shape.rect(mapBacking.width+mapBacking.x-borderWidth, mapBacking.y, borderWidth, mapBacking.height);//right


        shape.setColor(Color.WHITE);
        shape.circle(centerMapX, centerMapY, 2);

        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);


        batch.begin();
        String mapString = (int) MyScreenAdapter.cam.position.x / chunkSize + ", " + (int) MyScreenAdapter.cam.position.y / chunkSize;
        if (!drawScaleTimer.canDoEvent()) {
            mapString += ": " + mapScale;
        }
        fontSmall.draw(batch, mapString, mapBacking.x + 10, mapBacking.y + mapBacking.height - fontSmall.getLineHeight());
        batch.end();
    }


    public void cycleMiniMapPosition() {
        miniMapPosition = miniMapPosition.next();
        updateMapPosition();
        System.out.println(miniMapPosition);
    }

    public void cycleMapState() {
        mapState = mapState.next();
        updateMapPosition();
        System.out.println(mapState);
    }

    public void updateMapPosition() {
        mapBacking = getMiniMapRectangle();
    }

    private Rectangle getMiniMapRectangle() {
        if (mapState == MapState.full) {
            int edgePad = 50;
            return new Rectangle(edgePad, edgePad, Gdx.graphics.getWidth() - edgePad * 2, Gdx.graphics.getHeight() - edgePad * 2);
        } else {
            int miniWidth = 320;
            int miniHeight = 240;
            switch (miniMapPosition) {
                case topLeft:
                    return new Rectangle(
                            10,
                            Gdx.graphics.getHeight() - miniHeight - 10,
                            miniWidth,
                            miniHeight);
                case topRight:
                    return new Rectangle(
                            Gdx.graphics.getWidth() - miniWidth - 10,
                            Gdx.graphics.getHeight() - miniHeight - 10,
                            miniWidth,
                            miniHeight);
                case bottomLeft:
                    return new Rectangle(
                            10,
                            10,
                            miniWidth,
                            miniHeight);
                case bottomRight:
                    return new Rectangle(
                            Gdx.graphics.getWidth() - miniWidth - 10,
                            10,
                            miniWidth,
                            miniHeight);
            }
        }
        return new Rectangle();
    }

    public void scrollMiniMap(int amount) {
        mapScale += amount*20;
        drawScaleTimer.reset();
        System.out.println("map scale: " + mapScale);
    }

    public void resetMapScale() {
        mapScale = SpaceProject.uicfg.mapScale;
    }
}

