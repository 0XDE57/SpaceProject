package com.spaceproject.ui.debug.nodes;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.spaceproject.ui.debug.ECSExplorerWindow;
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
            newTimer = new SimpleTimer(ECSExplorerWindow.newTime, true);
            getActor().setColor(Color.GREEN);
        }
    }
    
    public Entity getEntity() {
        return (Entity) getValue();
    }
    
    @Override
    public void update() {
        if (getValue() == null) {
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
        
        boolean showHistory = ECSExplorerWindow.showHistory;
        
        //add nodes
        ImmutableArray<Component> components = getEntity().getComponents();
        for (Component comp : components) {
            if (findNode(comp) == null) {
                add(new ReflectionNode(comp, showHistory));
            }
        }
        
        //update nodes, clean up dead nodes
        for (Object child : getChildren()) {
            UpdateNode node = (UpdateNode)child;
            if (!components.contains((Component) node.getValue(), false)) {
                if (showHistory) {
                    if (!(node instanceof GhostNode)) {
                        node.removeAndCreateGhost(ECSExplorerWindow.includeChildren);
                    }
                } else {
                    node.remove();
                }
            } else {
                node.update();
            }
        }
    }
    
    @Override
    public String toString() {
        if (getValue() == null)
            return super.toString();
        
        return Misc.objString(getEntity()) + " [" + getEntity().getComponents().size() + "]";
    }
}
