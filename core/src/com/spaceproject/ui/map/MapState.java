package com.spaceproject.ui.map;

public enum MapState {
    full,
    mini,
    off;
    
    private static MapState[] vals = values();
    
    public MapState next() {
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
