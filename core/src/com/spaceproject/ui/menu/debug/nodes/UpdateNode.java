package com.spaceproject.ui.menu.debug.nodes;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.ui.menu.debug.DebugEngineWindow;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.SimpleTimer;

import java.lang.reflect.Field;

import static com.spaceproject.generation.FontFactory.skinSmallFont;


public abstract class UpdateNode extends Tree.Node {
    
    UpdateNode(Actor actor, Object obj) {
        super(actor);
        this.setObject(obj);
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
    
    public abstract void update();//update children?
    
    
    public void removeAndCreateGhost(boolean includeChildren) {
        setObject(null);//remove reference for GC, TODO: revisit this
        
        new GhostNode(this, includeChildren);
        
        remove();
    }
}

