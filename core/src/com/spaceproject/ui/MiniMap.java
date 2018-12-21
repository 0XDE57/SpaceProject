package com.spaceproject.ui;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.systems.SpaceLoadingSystem;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;
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

    public void drawSpaceMap(Engine engine, ShapeRenderer shape, SpriteBatch batch, Entity player, ImmutableArray<Entity> mapables) {
        if (mapState == MapState.off)
            return;

        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        /*
        Rectangle scissors = new Rectangle();
        Rectangle clipBounds = new Rectangle(mapBacking);
        //clipBounds.setPosition( GameScreen.cam.position.x + mapBacking.x, clipBounds.y - GameScreen.cam.position.y);
        ScissorStack.calculateScissors(GameScreen.cam, shape.getTransformMatrix(),clipBounds, scissors);
        ScissorStack.pushScissors(scissors);
        */

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
        if (mapState == MapState.full) {
            shape.setColor(1f, 0.2f, 0.2f, 1f);
            int mX = Gdx.input.getX();
            int mY = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (mapBacking.contains(mX, mY)) {
                shape.line(mapBacking.x, mY, mapBacking.x + mapBacking.width, mY);//horizontal
                shape.line(mX, mapBacking.y, mX, mapBacking.y + mapBacking.height);//vertical
            }
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

        boolean drawOrbit = false;//mapScale > 100;
        if (mapables != null) {
            for (Entity mapable : mapables) {




                MapComponent map = Mappers.map.get(mapable);
                Vector2 screenPos = Mappers.transform.get(mapable).pos;

                // n = relative pos / scale + mapPos
                float x = ((screenPos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                float y = ((screenPos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

                if (drawOrbit) {
                    OrbitComponent orbit = Mappers.orbit.get(mapable);
                    if (orbit != null && orbit.parent != null) {
                        TransformComponent parentPos = Mappers.transform.get(orbit.parent);
                        float xx = ((parentPos.pos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                        float yy = ((parentPos.pos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

                        //if (mapBacking.contains(xx, yy) && mapBacking.contains(x, y)) {
                            shape.setColor(0.5f, 0.5f, 0.5f, 0.5f);
                            shape.circle(parentPos.pos.x, parentPos.pos.y, orbit.radialDistance);
                            shape.line(xx, yy, x, y);//actual position
                        //}
                        //Vector2 test = new (Vector2.)
                    }
                }

                if (mapBacking.contains(x, y)) {
                    shape.setColor(map.color);
                    shape.circle(x, y, 2);
                }
            }
        }


        //draw border
        shape.setColor(0.6f,0.6f,0.6f,1f);
        shape.rect(mapBacking.x, mapBacking.height+mapBacking.y-borderWidth, mapBacking.width, borderWidth);//top
        shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, borderWidth);//bottom
        shape.rect(mapBacking.x, mapBacking.y, borderWidth, mapBacking.height);//left
        shape.rect(mapBacking.width+mapBacking.x-borderWidth, mapBacking.y, borderWidth, mapBacking.height);//right


        //draw velocity vector for intuitive navigation
        if (player != null) {
            TransformComponent t = Mappers.transform.get(player);

            //calculate vector angle and length
            float scale = 4; //how long to make vectors (higher number is longer line)
            float length = (float)Math.log(t.velocity.len()) * scale;
            float angle = t.velocity.angle() * MathUtils.degreesToRadians;
            Vector2 vel = MyMath.Vector(angle, length).add(centerMapX, centerMapY);

            //draw line to represent movement
            shape.rectLine(centerMapX, centerMapY, vel.x, vel.y, 2, Color.MAGENTA, Color.RED);

            Vector2 facing = MyMath.Vector(t.rotation, 10).add(centerMapX, centerMapY);
            shape.rectLine(centerMapX, centerMapY, facing.x, facing.y, 2, Color.GRAY, Color.WHITE);
        }
        shape.setColor(Color.WHITE);
        shape.circle(centerMapX, centerMapY, 3);


        shape.end();
        //ScissorStack.popScissors();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (mapState == MapState.full) {
            batch.begin();
            String mapString = (int) MyScreenAdapter.cam.position.x / chunkSize + ", " + (int) MyScreenAdapter.cam.position.y / chunkSize;
            if (!drawScaleTimer.canDoEvent()) {
                mapString += ": " + mapScale;
            }
            fontSmall.draw(batch, mapString, mapBacking.x + 10, mapBacking.y + mapBacking.height - fontSmall.getLineHeight());
            batch.end();
        }


    }


    public void cycleMiniMapPosition() {
        miniMapPosition = miniMapPosition.next();
        updateMapPosition();
        Gdx.app.log(this.getClass().getSimpleName(), miniMapPosition.toString());
    }

    public void cycleMapState() {
        mapState = mapState.next();
        updateMapPosition();
        Gdx.app.log(this.getClass().getSimpleName(), mapState.toString());
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
        //TODO: make this some log function, small increment when zoom in and larger the further out
        float changeLarge = 20, changeSmall = 2;
        mapScale += amount * ((mapScale >= changeLarge) ? changeLarge : changeSmall);
        drawScaleTimer.reset();
        Gdx.app.log(this.getClass().getSimpleName(), "map scale: " + mapScale);
    }

    public void resetMapScale() {
        mapScale = SpaceProject.uicfg.mapScale;
    }
}

