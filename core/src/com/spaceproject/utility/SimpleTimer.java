package com.spaceproject.utility;

import com.spaceproject.screens.GameScreen;

public class SimpleTimer {

    private long interval;
    private long lastEvent;
    
    public SimpleTimer(long time) {
        this(time, false);
    }

    public SimpleTimer(long time, boolean setLastEventTime) {
        interval = time;
        if (setLastEventTime)
            reset();
    }

    public boolean tryEvent() {
        if (canDoEvent()) {
            reset();
            return true;
        }
        return false;
    }

    public boolean canDoEvent() {
        return timeSinceLastEvent() >= interval;
    }

    public void reset() {
        lastEvent = GameScreen.getGameTimeCurrent();
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval, boolean reset) {
        this.interval = interval;
        if (reset)
            reset();
    }

    public void setLastEvent(long time) {
        lastEvent = time;
    }

    public long timeSinceLastEvent() {
        return GameScreen.getGameTimeCurrent() - lastEvent;
    }

    public float ratio() {
        return Math.min((float)timeSinceLastEvent()/(float)interval, 1.0f);
    }
    
    @Override
    public String toString() {
        return timeSinceLastEvent() + " (" + ratio() + ")";
    }
}
