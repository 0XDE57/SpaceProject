package com.spaceproject.ui.map;

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
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.MiniMapConfig;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.SimpleTimer;


public class MiniMap {
    
    private MiniMapConfig miniMapCFG;
    private CelestialConfig celestCFG;
    public MapState mapState = MapState.mini;
    private MiniMapPosition miniMapPosition = MiniMapPosition.bottomRight; //todo: save cfg preference?
    
    private Rectangle mapBacking;
    private boolean debugDisableClipping = false;//TODO: move to cfg
    private boolean debugDrawLoadDist = false;//todo cfg!
    int fontSize = 12; //todo cfg
    
    private BitmapFont font;
    private SimpleTimer drawScaleTimer;
    private float mapScale;
    
    public MiniMap(MiniMapConfig miniMapConfig, CelestialConfig celestialConfig) {
        miniMapCFG = miniMapConfig;
        celestCFG = celestialConfig;
        
        if (SpaceProject.isMobile()) {
            miniMapPosition = MiniMapPosition.topLeft;
        }
        drawScaleTimer = new SimpleTimer(miniMapCFG.drawScaleTimer);
        
        updateMapPosition();
        resetMapScale();
        
        font = FontFactory.createFont(FontFactory.fontPressStart, fontSize);
    }
    
    public void drawSpaceMap(ShapeRenderer shape, SpriteBatch batch, Entity player, ImmutableArray<Entity> entities) {
        if (mapState == MapState.off)
            return;
        
        if (!debugDisableClipping) {
            ScissorStack.pushScissors(mapBacking);
        }
        
        float centerMapX = mapBacking.x + mapBacking.width / 2;
        float centerMapY = mapBacking.y + mapBacking.height / 2;
        
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        {
            //draw backing
            shape.setColor(miniMapCFG.backingColor);
            shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, mapBacking.height);
            
            
            //draw mouse pos
            if (mapState == MapState.full) {
                shape.setColor(miniMapCFG.mouseColor);
                int mX = Gdx.input.getX();
                int mY = Gdx.graphics.getHeight() - Gdx.input.getY();
                if (mapBacking.contains(mX, mY)) {
                    shape.line(mapBacking.x, mY, mapBacking.x + mapBacking.width, mY);//horizontal
                    shape.line(mX, mapBacking.y, mX, mapBacking.y + mapBacking.height);//vertical
                }
            }
            
            
            //draw grid X
            shape.setColor(miniMapCFG.gridColor);
            int halfWidth = (int) (((mapBacking.width / 2)));
            int startX = (int) ((-halfWidth * mapScale) + MyScreenAdapter.cam.position.x) / miniMapCFG.gridSize;
            int endX = (int) ((halfWidth * mapScale) + MyScreenAdapter.cam.position.x) / miniMapCFG.gridSize;
            for (int i = startX; i < endX + 1; i++) {
                float finalX = (((i * miniMapCFG.gridSize) - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                shape.rect(finalX, mapBacking.y, 1, mapBacking.height);
            }
            
            // draw grid Y
            int halfHeight = (int) (((mapBacking.height / 2)));
            int startY = (int) ((-halfHeight * mapScale) + MyScreenAdapter.cam.position.y) / miniMapCFG.gridSize;
            int endY = (int) ((halfHeight * mapScale) + MyScreenAdapter.cam.position.y) / miniMapCFG.gridSize;
            for (int i = startY; i < endY + 1; i++) {
                float finalY = (((i * miniMapCFG.gridSize) - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
                shape.rect(mapBacking.x, finalY, mapBacking.width, 1);
            }
        }
        shape.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        {
            boolean drawOrbit = mapScale <= miniMapCFG.lodRenderOrbitPathScale;
            if (drawOrbit) {
                if (entities != null) {
                    shape.setColor(miniMapCFG.orbitPath);
                    for (Entity mapEntity : entities) {
                        
                        Vector2 screenPos = Mappers.transform.get(mapEntity).pos;
                        
                        // n = relative pos / scale + mapPos
                        float x = ((screenPos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                        float y = ((screenPos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
                        
                        OrbitComponent orbit = Mappers.orbit.get(mapEntity);
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
            
            //debug
            if (debugDrawLoadDist) {
                shape.setColor(miniMapCFG.debugLoadDistColor);
                if (GameScreen.inSpace()) {
                    for (Vector2 p : GameScreen.universe.points) {
                        // n = relative pos / scale + mapPos
                        float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                        float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
                        
                        shape.circle(x, y, celestCFG.loadSystemDistance / mapScale);
                    }
                }
            }
        }
        shape.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        {
            //draw all celestial bodies
            if (GameScreen.inSpace()) {
                float loadDist = celestCFG.loadSystemDistance;
                loadDist *= loadDist;
                for (Vector2 p : GameScreen.universe.points) {
                    if (p.dst2(MyScreenAdapter.cam.position.x, MyScreenAdapter.cam.position.y) < loadDist) {
                        continue;
                    }
                    
                    // n = relative pos / scale + mapPos
                    float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                    float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
                    
                    if (mapBacking.contains(x, y)) {
                        shape.setColor(1, 1, 1, 1);//TODO: dynamic color based on celestial body type
                        shape.circle(x, y, miniMapCFG.celestialMarkerSize);
                    }
                }
            }
            
            //draw loaded celestial bodies
            if (entities != null) {
                for (Entity mapEntity : entities) {
                    MapComponent map = Mappers.map.get(mapEntity);
                    Vector2 screenPos = Mappers.transform.get(mapEntity).pos;
                    
                    // n = relative pos / scale + mapPos
                    float x = ((screenPos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                    float y = ((screenPos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
                    
                    if (mapBacking.contains(x, y)) {
                        shape.setColor(map.color);
                        float size = 2;
                        TextureComponent tex = Mappers.texture.get(mapEntity);
                        if (tex != null) {
                            size = Math.max((tex.texture.getWidth() / 2.0f * tex.scale) / mapScale, 1f);
                        }
                        shape.circle(x, y, size);
                    }
                }
            }
            
            
            //draw velocity vector for intuitive navigation
            //todo: move values to cfg
            if (player != null) {
                Body body = Mappers.physics.get(player).body;
                
                //calculate vector angle and length
                float scale = 5; //how long to make vectors (higher number is longer line)
                Vector2 vel = MyMath.logVec(body.getLinearVelocity(), scale).add(centerMapX, centerMapY);
                
                //draw line to represent movement
                shape.rectLine(centerMapX, centerMapY, vel.x, vel.y, 2, Color.MAGENTA, Color.RED);
                
                Vector2 facing = MyMath.vector(body.getAngle(), 10).add(centerMapX, centerMapY);
                shape.rectLine(centerMapX, centerMapY, facing.x, facing.y, 2, Color.GRAY, Color.WHITE);
            }
            shape.setColor(Color.WHITE);
            shape.circle(centerMapX, centerMapY, 3);
            
            
            //draw border
            shape.setColor(miniMapCFG.borderColor);
            shape.rect(mapBacking.x, mapBacking.height + mapBacking.y - miniMapCFG.borderWidth, mapBacking.width, miniMapCFG.borderWidth);//top
            shape.rect(mapBacking.x, mapBacking.y, mapBacking.width, miniMapCFG.borderWidth);//bottom
            shape.rect(mapBacking.x, mapBacking.y, miniMapCFG.borderWidth, mapBacking.height);//left
            shape.rect(mapBacking.width + mapBacking.x - miniMapCFG.borderWidth, mapBacking.y, miniMapCFG.borderWidth, mapBacking.height);//right
        }
        shape.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        if (!debugDisableClipping) {
            ScissorStack.popScissors();
        }
        
        
        batch.begin();
        {
            float textPosX = mapBacking.x + 10;
            float textPosY = mapBacking.y + mapBacking.height;
            float lineHeight = font.getLineHeight() + 2;
            
            String mapString = (int) MyScreenAdapter.cam.position.x + ", " + (int) MyScreenAdapter.cam.position.y;
            if (player != null) {
                Body body = Mappers.physics.get(player).body;
                String playerInfo = ": " + MyMath.round(body.getLinearVelocity().len(), 1);
                mapString += playerInfo;
            }
            font.draw(batch, mapString, textPosX, textPosY - lineHeight);
            
            
            if (mapState == MapState.full && !drawScaleTimer.canDoEvent()) {
                font.draw(batch, "scale: " + mapScale, centerMapX, textPosY - lineHeight);
            }
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
    }
    
    
    private Rectangle getMiniMapRectangle() {
        if (mapState == MapState.full) {
            return new Rectangle(miniMapCFG.edgePad, miniMapCFG.edgePad, Gdx.graphics.getWidth() - miniMapCFG.edgePad * 2, Gdx.graphics.getHeight() - miniMapCFG.edgePad * 2);
        } else {
            
            switch (miniMapPosition) {
                case topLeft:
                    return new Rectangle(miniMapCFG.miniEdgePad, Gdx.graphics.getHeight() - miniMapCFG.miniHeight - miniMapCFG.miniEdgePad, miniMapCFG.miniWidth, miniMapCFG.miniHeight);
                case topRight:
                    return new Rectangle(Gdx.graphics.getWidth() - miniMapCFG.miniWidth - miniMapCFG.miniEdgePad, Gdx.graphics.getHeight() - miniMapCFG.miniHeight - miniMapCFG.miniEdgePad, miniMapCFG.miniWidth, miniMapCFG.miniHeight);
                case bottomLeft:
                    return new Rectangle(miniMapCFG.miniEdgePad, miniMapCFG.miniEdgePad, miniMapCFG.miniWidth, miniMapCFG.miniHeight);
                case bottomRight:
                    return new Rectangle(Gdx.graphics.getWidth() - miniMapCFG.miniWidth - miniMapCFG.miniEdgePad, miniMapCFG.miniEdgePad, miniMapCFG.miniWidth, miniMapCFG.miniHeight);
            }
        }
        return new Rectangle();
    }
    
    
    public void scrollMiniMap(int amount) {
        float scrollAmount = amount * mapScale / miniMapCFG.zoomMultiplier;
        mapScale += scrollAmount;
        mapScale = MathUtils.clamp(mapScale, miniMapCFG.minScale, miniMapCFG.maxSale);
        
        drawScaleTimer.reset();
    }
    
    
    public boolean scrolled(int amount) {
        switch (mapState) {
            case full:
                scrollMiniMap(amount);
                return true;
            case mini:
                if (mapBacking.contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
                    scrollMiniMap(amount);
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    
    public void resetMapScale() {
        mapScale = miniMapCFG.mapScale;
        drawScaleTimer.reset();
    }
    
}

