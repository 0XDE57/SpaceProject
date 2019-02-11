package com.spaceproject.config;


public class SysCFG {
    private final String className;
    private final int priority;
    private final boolean haltOnGamePause;
    private final boolean loadInSpace;
    private final boolean loadInWorld;
    private final boolean loadOnDesktop;
    private final boolean loadOnMobile;
    //how to handle subscription events?
    //how would combat system subscribe to on collide
    //how would world subscribe to finished loading, space subscribe to planetsync?
    //private final boolean notifyResize;?
    //private final int inputProcessorPriority;?
    //probably not here. make each one implement a listener for that
    //input eg: for each loaded system, if class instanceOf InputProcessor: inputMultiplexor.addprocessor(class)
    //private final Config systemSpecificConfig? pass this into system itself to load its specific settings? should be separate from this?
    //eg minimapConfig { thiscolor, thatcolor, showthis, showThat }
    
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
