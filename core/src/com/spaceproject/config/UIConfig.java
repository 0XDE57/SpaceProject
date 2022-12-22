package com.spaceproject.config;

import com.badlogic.gdx.graphics.Color;

public class UIConfig extends Config {
    
    public int playerHPBarY;
    public int playerHPBarWidth;
    public int playerHPBarHeight;
    public Color playerAmmoBarColor;
    public Color playerAmmoBarRechargeColor;
    
    public boolean renderFullHealth;
    public int entityHPbarLength;
    public int entityHPbarWidth;
    public int entityHPbarYOffset;
    public Color entityHPbarBackground;
    public float entityHPbarOpacity;
    
    public Color orbitObjectColor;
    public Color orbitSyncPosColor;
    public float lodShowOrbitPath;
    public int orbitFadeFactor;
    
    @Override
    public void loadDefault() {
        playerHPBarY = 55;
        playerHPBarWidth = 200;
        playerHPBarHeight = 12;
        playerAmmoBarColor = Color.MAGENTA;
        playerAmmoBarRechargeColor = Color.SLATE;
        
        renderFullHealth = false;
        entityHPbarLength = 40;
        entityHPbarWidth = 8;
        entityHPbarYOffset = -20;
        entityHPbarBackground = new Color(1, 1, 1, 0.5f);
        entityHPbarOpacity = 0.7f;
        
        orbitObjectColor = new Color(1, 1, 1, 1);
        orbitSyncPosColor = new Color(1, 0, 0, 1);
        lodShowOrbitPath = 150;
        orbitFadeFactor = 2;
    }
}
