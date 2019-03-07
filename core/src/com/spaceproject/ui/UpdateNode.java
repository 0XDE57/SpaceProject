package com.spaceproject.ui;

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
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.SimpleTimer;

import java.lang.reflect.Field;

import static com.spaceproject.screens.GameScreen.smallFont;


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

class EntityNode extends UpdateNode {
    
    boolean isNew = false;
    private SimpleTimer newTimer;
    
    public EntityNode(Entity entity, Skin skin) {
        super(new Label(Misc.objString(entity), skin, smallFont, Color.WHITE), entity);
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

class ReflectionNode extends UpdateNode {
    
    boolean isNew = false;
    private SimpleTimer newTimer;
    
    public ReflectionNode(Object object) {
        super(new Label(Misc.objString(object), VisUI.getSkin(), smallFont, Color.WHITE), object);
        init();
    }
    
    public ReflectionNode(Object object, boolean markNew) {
        this(object);
        
        isNew = markNew;
        if (isNew) {
            newTimer = new SimpleTimer(DebugEngineWindow.newTime, true);
            getActor().setColor(Color.GREEN);
        }
    }
    
    private void init() {
        for (Field f : getObject().getClass().getFields()) {
            add(new FieldNode(new Label("init", VisUI.getSkin(), smallFont, Color.WHITE), getObject(), f));
        }
    }
    
    @Override
    public void update() {
        if (isNew && newTimer.tryEvent()) {
            isNew = false;
            newTimer = null;
            getActor().setColor(Color.WHITE);
        }
        
        if (!isExpanded())
            return;
        
        for (Tree.Node node : getChildren())
            ((FieldNode) node).update();
        
    }
    
    @Override
    public String toString() {
        if (getObject() == null)
            return super.toString();
        
        return Misc.objString(getObject());
    }
}

class FieldNode extends UpdateNode {
    
    private Object owner;
    
    public FieldNode(Actor actor, Object owner, Field field) {
        super(actor, field);
        this.owner = owner;
        getActor().setName(toString());
        update();
    }
    
    private Object getOwner() {
        return owner;
    }
    
    @Override
    public void update() {
        ((Label) getActor()).setText(toString());
    }
    
    @Override
    public String toString() {
        if (getObject() == null || getOwner() == null) {
            return super.toString();
        }
        try {
            return String.format("%-14s %s", ((Field) getObject()).getName(), ((Field) getObject()).get(getOwner()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}

class GhostNode extends UpdateNode {
    
    private SimpleTimer removeTimer;
    
    public GhostNode(UpdateNode nodeRemoved, boolean includeChildren) {
        super(new Label(nodeRemoved.getActor().getName(), VisUI.getSkin(), smallFont, Color.RED), null);
        
        Tree.Node parent = nodeRemoved.getParent();
        final Array<Tree.Node> parentsSiblings;
        if (parent == null) {
            parentsSiblings = nodeRemoved.getTree().getRootNodes();
        } else {
            parentsSiblings = parent.getChildren();
        }
        //insert in same position as removed node
        int index = parentsSiblings.indexOf(nodeRemoved, false);
        parent.insert(index, this);
        
        
        removeTimer = new SimpleTimer(DebugEngineWindow.removeTime, true);
        
        setExpanded(nodeRemoved.isExpanded());
        if (includeChildren && isExpanded()) {
            addChildren(nodeRemoved.getChildren(), this);
        }
    }
    
    public void addChildren(Array<Tree.Node> children, Tree.Node root) {
        for (Tree.Node child : children) {
            addChildren(child.getChildren(), root);
            
            add(new Tree.Node(new Label(child.getActor().getName(), VisUI.getSkin(), smallFont, Color.RED)));
        }
    }
    
    @Override
    public void update() {
        tryRemove();
    }
    
    public void tryRemove() {
        /*
        if (isExpanded()) {
            //this should be if has focus/isSelection (including children) instead
            return;
        }*/
        
        if (removeTimer.canDoEvent())
            remove();
    }
    
}