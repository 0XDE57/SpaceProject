package com.spaceproject.config;


import com.badlogic.ashley.core.EntitySystem;

public class SysCFG {
    private String className;
    private int priority;
    private boolean haltOnGamePause;
    private boolean loadInSpace;
    private boolean loadInWorld;
    private boolean loadOnDesktop;
    private boolean loadOnMobile;
    
    public SysCFG() {
    }
    
    SysCFG(Class<? extends EntitySystem> systemClass, int priority, boolean haltOnGamePause, boolean loadInSpace, boolean loadInWorld, boolean loadOnDesktop, boolean loadOnMobile) {
        this(systemClass.getName(), priority, haltOnGamePause, loadInSpace, loadInWorld, loadOnDesktop, loadOnMobile);
    }
    
    SysCFG(String className, int priority, boolean haltOnGamePause, boolean loadInSpace, boolean loadInWorld, boolean loadOnDesktop, boolean loadOnMobile) {
        this.className = className;
        this.priority = priority;
        this.haltOnGamePause = haltOnGamePause;
        this.loadInSpace = loadInSpace;
        this.loadInWorld = loadInWorld;
        this.loadOnDesktop = loadOnDesktop;
        this.loadOnMobile = loadOnMobile;
    }
    
    public String getClassName() {
        return className;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean isHaltOnGamePause() {
        return haltOnGamePause;
    }
    
    public boolean isLoadInSpace() {
        return loadInSpace;
    }
    
    public boolean isLoadInWorld() {
        return loadInWorld;
    }
    
    public boolean isLoadOnDesktop() {
        return loadOnDesktop;
    }
    
    public boolean isLoadOnMobile() {
        return loadOnMobile;
    }
}
