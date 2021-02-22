package com.spaceproject.ui.debug.nodes;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.spaceproject.utility.Misc;


public abstract class UpdateNode extends MyNode {
    
    UpdateNode(Actor actor, Object obj) {
        super(actor);
        this.setValue(obj);
        if (obj != null) {
            getActor().setName(Misc.objString(obj));
        }
    }
    
    @Override
    public void setExpanded(boolean expanded) {
        if (expanded == isExpanded())
            return;
        
        super.setExpanded(expanded);
        update();
    }
    
    
    public abstract void update();
    
    
    public void removeAndCreateGhost(boolean includeChildren) {
        new GhostNode(this, includeChildren);
        
        remove();
    }
}

