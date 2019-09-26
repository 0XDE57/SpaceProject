package com.spaceproject.ui.menu.debug.nodes;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.spaceproject.ui.menu.debug.DebugEngineWindow;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.SimpleTimer;

import static com.spaceproject.generation.FontFactory.skinSmallFont;

/**
 * Created by Whilow Schock on 25/09/2019.
 */
public class EntityNode extends UpdateNode {
    
    boolean isNew = false;
    private SimpleTimer newTimer;
    
    public EntityNode(Entity entity, Skin skin) {
        super(new Label(Misc.objString(entity), skin, skinSmallFont, Color.WHITE), entity);
    }
    
    public EntityNode(Entity entity, Skin skin, boolean markNew) {
        this(entity, skin);
        
        isNew = markNew;
        if (isNew) {
            newTimer = new SimpleTimer(DebugEngineWindow.newTime, true);
            getActor().setColor(Color.GREEN);
        }
    }
    
    public Entity getEntity() {
        return (Entity) getObject();
    }
    
    @Override
    public void update() {
        if (getObject() == null) {
            return;
        }
        
        if (isNew && newTimer.tryEvent()) {
            isNew = false;
            newTimer = null;
            getActor().setColor(Color.WHITE);
        }
        
        ((Label) getActor()).setText(toString());
        
        if (!isExpanded())
            return;
        
        boolean showHistory = DebugEngineWindow.showHistory;
        
        //add nodes
        ImmutableArray<Component> components = getEntity().getComponents();
        for (Component comp : components) {
            if (findNode(comp) == null) {
                add(new ReflectionNode(comp, showHistory));
            }
        }
        
        //update nodes, clean up dead nodes
        for (Tree.Node node : getChildren()) {
            if (!components.contains((Component) node.getObject(), false)) {
                if (showHistory) {
                    if (!(node instanceof GhostNode)) {
                        ((UpdateNode) node).removeAndCreateGhost(DebugEngineWindow.includeChildren);
                    }
                } else {
                    node.remove();
                }
            } else {
                ((UpdateNode) node).update();
            }
        }
    }
    
    @Override
    public String toString() {
        if (getObject() == null)
            return super.toString();
        
        return Misc.objString(getEntity()) + " [" + getEntity().getComponents().size() + "]";
    }
}
