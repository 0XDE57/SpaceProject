package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class StatsComponent implements Component {

    //stats
    //todo: kills, deaths, resourcesCollected, creditsEarned, creditsSpent, etc...
    // should it collect per life and total on death?
    public long distanceTraveled;

    public int kills; //this life;
    //public int killsTotal; //across all lives?
    public int deaths;

    public long shotsFired;//total?

    public long shotsHit;

    public long damageDealt;

    public long damageTaken;

    public long resourcesCollected;

    public long resourcesLost;

    public long profit;

}
