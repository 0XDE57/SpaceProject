package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.utility.SimpleTimer;

public class CannonComponent implements Component {
    
    public Vector2 anchorVec; //offset relative to ship
    
    public float aimAngle; //relative to anchor

    public float aimOffset; //spray
    
    public float damage;

    public int baseRate;
    
    public int minRate;
    
    public float velocity;
    
    public float acceleration;
    
    public int size;
    
    public SimpleTimer timerFireRate;
    
    public SimpleTimer timerRechargeRate;
    
    public float multiplier;

    public float heat;

    public float cooldownRate;

    public float heatRate;

    public float heatInaccuracy;

    //stats
    //todo: move to StatComponent? kills, deaths, accuracy, shotsFired, resourcesCollected, creditsEarned, creditsSpent, etc...
    public long shotsFired;

    public long hits;

    public long damageDealt;

    public long lastHitTime;

}
