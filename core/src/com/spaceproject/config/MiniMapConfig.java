package com.spaceproject.config;


import com.badlogic.gdx.graphics.Color;

public class MiniMapConfig extends Config {
    
    public float mapScale;
    public int gridSize;
    public int borderWidth;
    public int celestialMarkerSize;
    public int lodRenderOrbitPathScale;
    public int edgePad;
    public int miniEdgePad;
    public int miniWidth;
    public int miniHeight;
    public int zoomMultiplier;
    public float minScale;
    public float maxSale;
    public long drawScaleTimer;
    
    public Color backingColor;
    public Color borderColor;
    public Color gridColor;
    public Color mouseColor;
    public Color orbitPath;
    public Color debugLoadDistColor;
    
    public void loadDefault() {
        mapScale = 500;
        gridSize = (int) Math.pow(2, 17 - 1);
        borderWidth = 3;
        celestialMarkerSize = 6;
        lodRenderOrbitPathScale = 500;
        edgePad = 50;
        miniEdgePad = 10;
        miniWidth = 320;
        miniHeight = 240;
        zoomMultiplier = 4;
        minScale = 0.5f;
        maxSale = 10000;
        drawScaleTimer = 5000;
    
        backingColor = new Color(0, 0, 0, 0.8f);
        borderColor = new Color(0.6f, 0.6f, 0.6f, 1f);
        gridColor = new Color(0.2f, 0.2f, 0.2f, 0.8f);
        mouseColor = new Color(1f, 0.2f, 0.2f, 1f);
        orbitPath = new Color(0.5f, 0.5f, 0.5f, 0.5f);
        debugLoadDistColor = new Color(1, 0, 0, 1);
    }
}
