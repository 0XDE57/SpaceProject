package com.spaceproject.ui.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.spaceproject.components.HyperDriveComponent;
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
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.MyMath;
import com.spaceproject.utility.SimpleTimer;


public class MiniMap {
    
    private MiniMapConfig miniMapCFG;
    private CelestialConfig celestCFG;
    public MapState mapState = MapState.mini;
    private Rectangle mapContainer;
    private BitmapFont font;
    private float mapScale;
    private SimpleTimer drawScaleTimer;
    private static final Circle tmpCircle = new Circle();
    
    
    public MiniMap(MiniMapConfig miniMapConfig, CelestialConfig celestialConfig) {
        miniMapCFG = miniMapConfig;
        celestCFG = celestialConfig;
        
        drawScaleTimer = new SimpleTimer(miniMapCFG.drawScaleTimer);
        
        updateMapPosition();
        resetMapScale();
        
        font = FontFactory.createFont(FontFactory.fontPressStart, miniMapCFG.fontSize);
    }
    
    
    public void drawMiniMap(ShapeRenderer shape, SpriteBatch batch, Entity player, ImmutableArray<Entity> entities) {
        if (mapState == MapState.off)
            return;
        
        if (!miniMapCFG.debugDisableClipping) {
            ScissorStack.pushScissors(mapContainer);
        }
        
        float centerMapX = mapContainer.x + mapContainer.width / 2;
        float centerMapY = mapContainer.y + mapContainer.height / 2;
        
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        {
            //draw backing
            shape.setColor(miniMapCFG.backingColor);
            shape.rect(mapContainer.x, mapContainer.y, mapContainer.width, mapContainer.height);
            
            
            //draw mouse pos
            if (mapState == MapState.full) {
                drawMouseGrid(shape, miniMapCFG.mouseColor);
            }
            
            
            //draw grid
            drawGrid(shape, centerMapX, centerMapY,  miniMapCFG.gridSize, mapContainer.width, miniMapCFG.gridColor);
        }
        shape.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        {
            boolean drawOrbit = mapScale <= miniMapCFG.lodRenderOrbitPathScale;
            if (drawOrbit) {
                drawOrbitPaths(shape, entities, centerMapX, centerMapY, miniMapCFG.orbitPathColor);
            }
            
            //debug
            if (miniMapCFG.debugDrawLoadDist) {
                drawDebugLoadDist(shape, centerMapX, centerMapY, celestCFG.loadSystemDistance, miniMapCFG.debugLoadDistColor);
            }
        }
        shape.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        {
            //draw all celestial bodies
            if (GameScreen.getInstance().inSpace()) {
                drawUniversePoints(shape, centerMapX, centerMapY, celestCFG.loadSystemDistance, miniMapCFG.celestialMarkerSize, miniMapCFG.universeMarkerColor);
            }
            
            //draw loaded celestial bodies
            if (entities != null) {
                drawMapableEntities(shape, entities, centerMapX, centerMapY);
            }
            
            
            drawPlayerMarker(shape, player, centerMapX, centerMapY, miniMapCFG.playerMarkerSize,  miniMapCFG.playerMarkerColor, miniMapCFG.velocityVecColor);
    
    
            //draw border
            shape.setColor(miniMapCFG.borderColor);
            shape.rect(mapContainer.x, mapContainer.height + mapContainer.y - miniMapCFG.borderWidth, mapContainer.width, miniMapCFG.borderWidth);//top
            shape.rect(mapContainer.x, mapContainer.y, mapContainer.width, miniMapCFG.borderWidth);//bottom
            shape.rect(mapContainer.x, mapContainer.y, miniMapCFG.borderWidth, mapContainer.height);//left
            shape.rect(mapContainer.width + mapContainer.x - miniMapCFG.borderWidth, mapContainer.y, miniMapCFG.borderWidth, mapContainer.height);//right
        }
        shape.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        if (!miniMapCFG.debugDisableClipping) {
            ScissorStack.popScissors();
        }
        
        
        batch.begin();
        {
            drawMapText(batch, player, centerMapX);
        }
        batch.end();
        
        
    }
    
    
    private void drawMapText(SpriteBatch batch, Entity player, float centerMapX) {
        float textPosX = mapContainer.x + 10;
        float textPosY = mapContainer.y + mapContainer.height;
        float lineHeight = font.getLineHeight() + 2;
        
        String mapString = (int) MyScreenAdapter.cam.position.x + ", " + (int) MyScreenAdapter.cam.position.y;
        if (player != null) {
            Body body = Mappers.physics.get(player).body;
            String playerInfo = ": " + MyMath.round(body.getLinearVelocity().len(), 1);
            HyperDriveComponent hyper = player.getComponent(HyperDriveComponent.class);
            if (hyper != null) {
                playerInfo = ": " + MyMath.round(hyper.velocity.len(), 1);
            }
            mapString += playerInfo;
        }
        font.draw(batch, mapString, textPosX, textPosY - lineHeight);
        if (mapState == MapState.full) {
            //draw game time
            font.draw(batch, Misc.formatDuration(GameScreen.getGameTimeCurrent()), textPosX, textPosY - lineHeight * 2);
            //draw seed
            long seed = GameScreen.getInstance().getSeed();
            if (!GameScreen.getInstance().inSpace()) {
                seed = GameScreen.getInstance().getPlanetSeed();
            }
            font.draw(batch, seed + "", textPosX, textPosY - mapContainer.height + lineHeight);
        }
        if (mapState == MapState.full && !drawScaleTimer.canDoEvent()) {
            font.draw(batch, "scale: " + mapScale, centerMapX, textPosY - lineHeight);
        }
    }
    
    
    private void drawPlayerMarker(ShapeRenderer shape, Entity player, float centerMapX, float centerMapY, int playerMarkerSize, Color playerMarkerColor, Color velocityVecColor) {
        if (player != null) {
            float scale = 5; //how long to make vectors (higher number is longer line)
            int vecWidth = 2;
            
            Body body = Mappers.physics.get(player).body;
            
            Vector2 velocity = MyMath.logVec(body.getLinearVelocity(), scale).add(centerMapX, centerMapY);
            shape.rectLine(centerMapX, centerMapY, velocity.x, velocity.y, vecWidth, velocityVecColor, velocityVecColor);
            
            Vector2 facing = MyMath.vector(body.getAngle(), 10).add(centerMapX, centerMapY);
            shape.rectLine(centerMapX, centerMapY, facing.x, facing.y, vecWidth, playerMarkerColor, playerMarkerColor);
        }
        shape.setColor(playerMarkerColor);
        shape.circle(centerMapX, centerMapY, playerMarkerSize);
    }
    
    
    private void drawMapableEntities(ShapeRenderer shape, ImmutableArray<Entity> entities, float centerMapX, float centerMapY) {
        for (Entity mapEntity : entities) {
            MapComponent map = Mappers.map.get(mapEntity);
            Vector2 screenPos = Mappers.transform.get(mapEntity).pos;
            
            // n = relative pos / scale + mapPos
            float x = ((screenPos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
            float y = ((screenPos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

            float size = 2;
            TextureComponent tex = Mappers.texture.get(mapEntity);
            if (tex != null) {
                size = Math.max((tex.texture.getWidth() * 0.5f * tex.scale) / mapScale, 1f);
            }
            
            tmpCircle.set(x, y, -size);// negative because want to include edges
            if (mapContainer.contains(tmpCircle)) {
                shape.setColor(map.color);
                shape.circle(x, y, size);
            }
        }
    }
    
    
    private void drawUniversePoints(ShapeRenderer shape, float centerMapX, float centerMapY, float loadDist, int celestialMarkerSize, Color color) {
        loadDist *= loadDist;
        for (Vector2 p : GameScreen.universe.points) {
            if (p.dst2(MyScreenAdapter.cam.position.x, MyScreenAdapter.cam.position.y) < loadDist) {
                continue;
            }
            
            // n = relative pos / scale + mapPos
            float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
            float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
            
            if (mapContainer.contains(x, y)) {
                shape.setColor(color);//TODO: dynamic color based on celestial body type
                shape.circle(x, y, celestialMarkerSize);
            }
        }
    }
    
    
    private void drawOrbitPaths(ShapeRenderer shape, ImmutableArray<Entity> entities, float centerMapX, float centerMapY, Color color) {
        if (entities != null) {
            shape.setColor(color);
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
    
    
    private void drawMouseGrid(ShapeRenderer shape, Color color) {
        shape.setColor(color);
        int mX = Gdx.input.getX();
        int mY = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (mapContainer.contains(mX, mY)) {
            shape.line(mapContainer.x, mY, mapContainer.x + mapContainer.width, mY);//horizontal
            shape.line(mX, mapContainer.y, mX, mapContainer.y + mapContainer.height);//vertical
        }
    }
    
    
    private void drawGrid(ShapeRenderer shape, float centerMapX, float centerMapY, int gridSize, float width, Color gridColor) {
        shape.setColor(gridColor);
        int halfWidth = (int) (((width / 2)));
        int startX = (int) ((-halfWidth * mapScale) + MyScreenAdapter.cam.position.x) / gridSize;
        int endX = (int) ((halfWidth * mapScale) + MyScreenAdapter.cam.position.x) / gridSize;
        for (int i = startX; i < endX + 1; i++) {
            float finalX = (((i * gridSize) - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
            shape.rect(finalX, mapContainer.y, 1, mapContainer.height);
        }
        
        // draw grid Y
        int halfHeight = (int) (((mapContainer.height / 2)));
        int startY = (int) ((-halfHeight * mapScale) + MyScreenAdapter.cam.position.y) / gridSize;
        int endY = (int) ((halfHeight * mapScale) + MyScreenAdapter.cam.position.y) / gridSize;
        for (int i = startY; i < endY + 1; i++) {
            float finalY = (((i * gridSize) - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
            shape.rect(mapContainer.x, finalY, width, 1);
        }
    }
    
    
    private void drawDebugLoadDist(ShapeRenderer shape, float centerMapX, float centerMapY, float loadSystemDistance, Color color) {
        shape.setColor(color);
        if (GameScreen.getInstance().inSpace()) {
            for (Vector2 p : GameScreen.universe.points) {
                // n = relative pos / scale + mapPos
                float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
                shape.circle(x, y, loadSystemDistance / mapScale);
            }
        }
    }
    
    
    public void cycleMiniMapPosition() {
        miniMapCFG.miniMapPosition = miniMapCFG.miniMapPosition.next();
        updateMapPosition();
    }
    
    
    public void cycleMapState() {
        mapState = mapState.next();
        updateMapPosition();
    }
    
    
    public void updateMapPosition() {
        mapContainer = getMiniMapRectangle();
        drawScaleTimer.reset();
    }
    
    
    private Rectangle getMiniMapRectangle() {
        if (mapState == MapState.full) {
            return new Rectangle(miniMapCFG.edgePad, miniMapCFG.edgePad, Gdx.graphics.getWidth() - miniMapCFG.edgePad * 2, Gdx.graphics.getHeight() - miniMapCFG.edgePad * 2);
        } else {
            
            switch (miniMapCFG.miniMapPosition) {
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
                if (mapContainer.contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
                    scrollMiniMap(amount);
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    
    public void resetMapScale() {
        mapScale = miniMapCFG.defaultMapScale;
        drawScaleTimer.reset();
    }
    
}

