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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
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

    private Rectangle mapBacking;
    private boolean debugDisableClipping = false;
    private boolean debugDrawLoadDist = false;

    private BitmapFont fontSmall;

    private SimpleTimer drawScaleTimer = new SimpleTimer(5000);


    private float mapScale;
    private int chunkSize;

    //todo: move properties to config
    private int borderWidth = 3;
    private int celestialMarkerSize = 6;
    private int lodRenderOrbitPathScale = 500;

    private Color backingColor = new Color(0, 0, 0, 0.8f);
    private Color borderColor = new Color(0.6f,0.6f,0.6f,1f);
    private Color gridColor = new Color(0.2f, 0.2f, 0.2f, 0.8f);
    private Color mouseColor = new Color(1f, 0.2f, 0.2f, 1f);
    private Color orbitPath = new Color(0.5f, 0.5f, 0.5f, 0.5f);
    private Color debugLoadDistColor = new Color(1, 0, 0, 1);

    public MiniMap() {
        //mapState = SpaceProject.isMobile() ? MapState.off : MapState.mini;
        //if (mobile) miniMapPosition = middleRight?//TODO: make minimap location for mobile that doesnt interfere/overlap controls


        updateMapPosition();
        resetMapScale();


        chunkSize = SpaceProject.uicfg.mapChunkSize;

        fontSmall = FontFactory.createFont(FontFactory.fontPressStart, 12);
    }

    public void drawSpaceMap(Engine engine, ShapeRenderer shape, SpriteBatch batch, Entity player, ImmutableArray<Entity> mapables) {
        if (mapState == MapState.off)
            return;

        if (!debugDisableClipping) {
            ScissorStack.pushScissors(mapBacking);
        }

        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);


        //draw backing
        shape.setColor(backingColor);
        shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, mapBacking.height);


        //draw mouse pos
        if (mapState == MapState.full) {
            shape.setColor(mouseColor);
            int mX = Gdx.input.getX();
            int mY = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (mapBacking.contains(mX, mY)) {
                shape.line(mapBacking.x, mY, mapBacking.x + mapBacking.width, mY);//horizontal
                shape.line(mX, mapBacking.y, mX, mapBacking.y + mapBacking.height);//vertical
            }
        }


        if (mapScale < 1)
            mapScale = 1;

        float centerMapX = mapBacking.x + mapBacking.width / 2;
        float centerMapY = mapBacking.y + mapBacking.height / 2;


        //draw grid X
        shape.setColor(gridColor);
        int halfWidth = (int)(((mapBacking.width / 2)));
        int startX = (int)((-halfWidth * mapScale) + MyScreenAdapter.cam.position.x)/chunkSize;
        int endX = (int)((halfWidth * mapScale) + MyScreenAdapter.cam.position.x)/chunkSize;
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

        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);


        boolean drawOrbit = mapScale <= lodRenderOrbitPathScale;
        if (drawOrbit) {
            if (mapables != null) {
                shape.setColor(orbitPath);
                for (Entity mapable : mapables) {

                    Vector2 screenPos = Mappers.transform.get(mapable).pos;

                    // n = relative pos / scale + mapPos
                    float x = ((screenPos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                    float y = ((screenPos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

                    OrbitComponent orbit = Mappers.orbit.get(mapable);
                    if (orbit != null && orbit.parent != null) {
                        TransformComponent parentPos = Mappers.transform.get(orbit.parent);
                        float xx = ((parentPos.pos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                        float yy = ((parentPos.pos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

                        shape.circle(xx, yy, orbit.radialDistance / mapScale);
                        shape.line(xx, yy, x, y);
                    }
                }
            }
        }


        float loadDist = SpaceProject.celestcfg.loadSystemDistance;
        //debug
        if (debugDrawLoadDist) {

            shape.setColor(debugLoadDistColor);
            SpaceLoadingSystem spaceLoader = engine.getSystem(SpaceLoadingSystem.class);
            if (spaceLoader != null) {
                for (Vector2 p : spaceLoader.getPoints()) {
                    // n = relative pos / scale + mapPos
                    float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                    float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

                    shape.circle(x, y, loadDist / mapScale);
                }
            }
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);

        //draw all celestial bodies
        SpaceLoadingSystem spaceLoader = engine.getSystem(SpaceLoadingSystem.class);
        if (spaceLoader != null) {
            for (Vector2 p : spaceLoader.getPoints()) {
                if (p.dst2(MyScreenAdapter.cam.position.x, MyScreenAdapter.cam.position.y) < (loadDist * loadDist)) {
                    continue;
                }

                // n = relative pos / scale + mapPos
                float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

                if (mapBacking.contains(x, y)) {
                    shape.setColor(1, 1, 1, 1);//TODO: dynamic color based on celestial body type
                    shape.circle(x, y, celestialMarkerSize);
                }
            }
        }

        //draw loaded celestial bodies
        if (mapables != null) {
            for (Entity mapable : mapables) {
                MapComponent map = Mappers.map.get(mapable);
                Vector2 screenPos = Mappers.transform.get(mapable).pos;

                // n = relative pos / scale + mapPos
                float x = ((screenPos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                float y = ((screenPos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

                if (mapBacking.contains(x, y)) {
                    shape.setColor(map.color);
                    shape.circle(x, y, 2);//TODO: dynamic size based on celestial size
                }
            }
        }


        //draw velocity vector for intuitive navigation
        if (player != null) {
            TransformComponent t = Mappers.transform.get(player);

            //calculate vector angle and length
            float scale = 4; //how long to make vectors (higher number is longer line)
            Vector2 vel = MyMath.LogVec(t.velocity, scale).add(centerMapX, centerMapY);
            Vector2 accel = MyMath.LogVec(t.accel, scale).add(centerMapX, centerMapY);

            //draw line to represent movement
            shape.rectLine(centerMapX, centerMapY, vel.x, vel.y, 2, Color.MAGENTA, Color.RED);
            shape.rectLine(centerMapX, centerMapY, accel.x, accel.y, 2, Color.GREEN, Color.BLUE);

            Vector2 facing = MyMath.Vector(t.rotation, 10).add(centerMapX, centerMapY);
            shape.rectLine(centerMapX, centerMapY, facing.x, facing.y, 2, Color.GRAY, Color.WHITE);
        }
        shape.setColor(Color.WHITE);
        shape.circle(centerMapX, centerMapY, 3);


        //draw border
        shape.setColor(borderColor);
        shape.rect(mapBacking.x, mapBacking.height+mapBacking.y-borderWidth, mapBacking.width, borderWidth);//top
        shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, borderWidth);//bottom
        shape.rect(mapBacking.x, mapBacking.y, borderWidth, mapBacking.height);//left
        shape.rect(mapBacking.width+mapBacking.x-borderWidth, mapBacking.y, borderWidth, mapBacking.height);//right


        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (!debugDisableClipping) {
            ScissorStack.popScissors();
        }


        batch.begin();
        float textPosX = mapBacking.x + 10;
        float textPosY = mapBacking.y + mapBacking.height;
        float lineHeight = fontSmall.getLineHeight() + 2;

        String mapString = (int)MyScreenAdapter.cam.position.x + ", " + (int)MyScreenAdapter.cam.position.y;
        if (player != null) {
            TransformComponent t = Mappers.transform.get(player);
            String playerInfo = ": " + (int)t.velocity.len() + "";
            mapString += playerInfo;
        }
        fontSmall.draw(batch, mapString, textPosX, textPosY - lineHeight);



        if (mapState == MapState.full && !drawScaleTimer.canDoEvent()) {
            fontSmall.draw(batch, "scale: " + mapScale, centerMapX, textPosY - lineHeight);
        }

        batch.end();


    }


    public void cycleMiniMapPosition() {
        miniMapPosition = miniMapPosition.next();
        updateMapPosition();
    }

    public void cycleMapState() {
        mapState = mapState.next();
        updateMapPosition();
    }

    public void updateMapPosition() {
        mapBacking = getMiniMapRectangle();
        drawScaleTimer.reset();
        Gdx.app.log(this.getClass().getSimpleName(), mapState.toString() + ", " + miniMapPosition.toString() + ", " + mapBacking.toString());
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
        drawScaleTimer.reset();
    }
}

