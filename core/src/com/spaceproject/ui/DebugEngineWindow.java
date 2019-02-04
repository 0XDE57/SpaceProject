package com.spaceproject.ui;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.utils.Selection;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.spaceproject.generation.FontFactory;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.SimpleTimer;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;


public class DebugEngineWindow extends VisWindow implements EntityListener {

    Engine engine;
    Tree tree;
    Node systemNodes, entityNodes;
    Skin skin;

    SimpleTimer refreshRate = new SimpleTimer(1000, true);
    static boolean showHistory = false;

    final int keyUP = Input.Keys.UP;
    final int keyDown = Input.Keys.DOWN;
    final int keyLeft = Input.Keys.LEFT;
    final int keyRight = Input.Keys.RIGHT;
    
    static String smallFont = "smallFont";

    public DebugEngineWindow(Engine engine) {
        super("Debug Engine View");

        this.engine = engine;


        setResizable(true);
        setMovable(true);
        setSize(400, Gdx.graphics.getHeight()-20);
        setPosition(10,10);
        addCloseButton();
        
        

        //setKeepWithinParent(true);

        TableUtils.setSpacingDefaults(this);
        columnDefaults(0).left();

        
        skin = VisUI.getSkin();
        BitmapFont font = FontFactory.createFont(FontFactory.fontBitstreamVM, 12);
        
        skin.add(smallFont, font);
        
        systemNodes = new Node(new Label("Systems", skin, smallFont, Color.WHITE));
        entityNodes = new Node(new Label("Entities", skin, smallFont, Color.WHITE));
        tree = new Tree(skin);
        tree.add(systemNodes);
        tree.add(entityNodes);
        refreshNodes();


        ScrollPane scrollPane = new ScrollPane(tree);
        //scrollPane.setFlickScroll(false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollbarsOnTop(true);
        //scrollPane.scrol
        add(scrollPane).expand().fill();

    }

    public void refreshNodes() {
        if (!isVisible()) {
            return;
        }
        
        if (refreshRate.tryEvent()) {
            if (showHistory) {
                //clearGhosts(entityNodes.getChildren());
            }
            updateSystems();
            updateEntities();
        }
    }
    
    private void updateEntities() {
        //update entities
        for (Entity entity : engine.getEntities()) {
            Node entNode = entityNodes.findNode(entity);
            if (entNode == null) {
                entityNodes.add(new EntityNode(entity, skin));
            } else {
                ((UpdateNode)entNode).update();
            }
        }
    }
    
    private void updateSystems() {
        //add nodes
        for (EntitySystem system : engine.getSystems()) {
            Node sysNode = systemNodes.findNode(system);
            if (sysNode == null) {
                systemNodes.add(new ReflectionNode(system));
            }
        }
        
        //update nodes, clean up dead nodes
        for (Node node : systemNodes.getChildren()) {
            if (!engine.getSystems().contains((EntitySystem) node.getObject(),false)) {
                node.remove();
            } else {
                ((ReflectionNode)node).update();
            }
        }
    }
    
    private void clearGhosts(Array<Node> nodes) {
        for (Node child : nodes) {
            if (child.getChildren().size > 0) {
                clearGhosts(child.getChildren());
            }
            
            if (child instanceof GhostNode) {
                ((GhostNode) child).tryRemove();
            }
        }
    }
    //region menu controls
    public boolean isVisible() {
        return getStage() != null;
    }

    public void show(Stage stage) {
        stage.addActor(this);
        fadeIn();
        
        engine.addEntityListener(this);
        refreshNodes();
    }

    public void hide() {
        fadeOut();

        engine.removeEntityListener(this);
        systemNodes.removeAll();
        entityNodes.removeAll();
    }

    public void toggle(Stage stage) {
        if (isVisible()) {
            hide();
        } else {
            show(stage);
        }
    }

    public void keyDown(InputEvent event, int keycode) {
        Selection<Node> nodeSelection = tree.getSelection();
        Node selectedNode = nodeSelection.first();
        if (selectedNode == null) {
            List<Integer> keys = Arrays.asList(keyUP, keyDown, keyLeft, keyRight);
            if (keys.contains(keycode)) {
                nodeSelection.choose(tree.getRootNodes().first());
            }
            return;
        }

        final Array<Node> siblingNodes;
        Node parentNode = selectedNode.getParent();
        if (parentNode == null) {
            siblingNodes = tree.getRootNodes();
        } else {
            siblingNodes = parentNode.getChildren();
        }
        int index = siblingNodes.indexOf(selectedNode, false);
        
        Node chosen = null;
        switch (keycode) {
            case keyUP:
                if (index > 0) {
                    chosen = getNextNodeAbove(siblingNodes.get(index - 1));
                } else {
                    if (parentNode != null) {
                        chosen = parentNode;
                    }
                }
                break;
            case keyDown:
                if (selectedNode.isExpanded() && (selectedNode.getChildren().size > 0)) {
                    chosen = selectedNode.getChildren().first();
                } else {
                    chosen = getNextNodeBelow(siblingNodes, parentNode, index);
                }
                break;
            case keyLeft:
                if (selectedNode.isExpanded()) {
                    selectedNode.collapseAll();
                } else {
                    if (parentNode != null) {
                        chosen = parentNode;
                    }
                }
                break;
            case keyRight:
                if (!selectedNode.isExpanded()) {
                    selectedNode.setExpanded(true);
                }
                break;
        }
    
        if (chosen != null) {
            nodeSelection.choose(chosen);
        }
    }
    
    private Node getNextNodeAbove(Node current) {
        if (current.isExpanded()) {
            if (current.getChildren().size > 0) {
                Node nextNode = current.getChildren().get(current.getChildren().size-1);
                return getNextNodeAbove(nextNode);
            }
        }
        
        return current;
    }
    
    private Node getNextNodeBelow(Array<Node> siblingNodes, Node parentNode, int index) {
        if (index < siblingNodes.size - 1) {
            return siblingNodes.get(index + 1);
        } else if (index == siblingNodes.size - 1) {
            if (parentNode != null) {
                Node parentsParent = parentNode.getParent(); //(grandparents?)
                final Array<Node> parentsSiblings; //(aunts/uncles?)
                if (parentsParent == null) {
                    parentsSiblings = tree.getRootNodes();
                } else {
                    parentsSiblings = parentsParent.getChildren();
                }
                index = parentsSiblings.indexOf(parentNode, false);
                return getNextNodeBelow(parentsSiblings, parentsParent, index);
            }
        }
        return null;
    }
    //endregion

    @Override
    public void entityAdded(Entity entity) {
        entityNodes.add(new EntityNode(entity, skin, showHistory));
    }

    @Override
    public void entityRemoved(Entity entity) {
        Node node = entityNodes.findNode(entity);
        if (showHistory) {
            ((UpdateNode)node).removeAndCreateGhost();
        } else {
            node.remove();
        }
        //entityNodes.findNode(entity).remove();
    }

}

class EntityNode extends UpdateNode {

    boolean isNew = false;
    private SimpleTimer newTimer;
    
    public EntityNode(Entity entity, Skin skin) {
        super(new Label("init", skin, DebugEngineWindow.smallFont, Color.WHITE), entity);
    }
    
    public EntityNode(Entity entity, Skin skin, boolean markNew) {
        this(entity, skin);

        isNew = markNew;
        if (isNew) {
            newTimer = new SimpleTimer(1000, true);
            getActor().setColor(Color.GREEN);
        }
    }

    public Entity getEntity() {
        return (Entity) getObject();
    }
    
    @Override
    public void update() {
        if (isNew && newTimer.tryEvent()){
            isNew = false;
            newTimer = null;
            getActor().setColor(Color.WHITE);
        }
        
        ((Label)getActor()).setText(toString());
        
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
        for (Node node : getChildren()) {
            if (!components.contains((Component)node.getObject(),false)) {
                if (showHistory) {
                    if (!(node instanceof GhostNode)) {
                        ((UpdateNode) node).removeAndCreateGhost();
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
        return Misc.myToString(getEntity()) + " [" + getEntity().getComponents().size() + "]";
    }
}

class ReflectionNode extends UpdateNode {
    
    boolean isNew = false;
    private SimpleTimer newTimer;
    
    public ReflectionNode(Object object) {
        super(new Label(Misc.myToString(object), VisUI.getSkin(), DebugEngineWindow.smallFont, Color.WHITE), object);
        init();
    }
    
    public ReflectionNode(Object object, boolean markNew) {
        this(object);
        
        isNew = markNew;
        if (isNew) {
            newTimer = new SimpleTimer(1000, true);
            getActor().setColor(Color.GREEN);
        }
    }
    
    private void init() {
        for (Field f : getObject().getClass().getFields()) {
            add(new FieldNode(new Label("init", VisUI.getSkin(), DebugEngineWindow.smallFont, Color.WHITE), getObject(), f));
        }
    }

    @Override
    public void update() {
        if (isNew && newTimer.tryEvent()){
            isNew = false;
            newTimer = null;
            getActor().setColor(Color.WHITE);
        }
        
        if (!isExpanded())
            return;
        
        for (Node node : getChildren())
            ((FieldNode) node).update();
        
    }
    
    @Override
    public String toString() {
        return Misc.myToString(getObject());
    }
}

class FieldNode extends UpdateNode {

    private Object obj;
    public FieldNode(Actor actor, Object obj, Field field) {
        super(actor, field);
        this.obj = obj;
        update();
    }
    
    private Object getObj() {
        return obj;
    }

    @Override
    public void update() {
        ((Label)getActor()).setText(toString());
    }
    
    @Override
    public String toString() {
        try {
            return String.format("\t\t%-14s %s", ((Field) getObject()).getName(), ((Field) getObject()).get(getObj()));
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
    public GhostNode(UpdateNode nodeRemoved) {
        super(nodeRemoved.getActor()/*.clone()???TODO*/, null);
        
        Node parent = nodeRemoved.getParent();
        final Array<Node> parentsSiblings;
        if (parent == null) {
            parentsSiblings = nodeRemoved.getTree().getRootNodes();
        } else {
            parentsSiblings = parent.getChildren();
        }
        int index = parentsSiblings.indexOf(nodeRemoved, false);
        
        //parentsSiblings.insert(index, this);
        //parent.add(this);
        parent.insert(index, this);//TODO: when adding, node doesn't update until new node added
        //parent.getTree().invalidateHierarchy();
        
        
        
        getActor().setColor(Color.RED);
        
        removeTimer = new SimpleTimer(1000, true);
        
        //getChildren() new GhostNode(child)
        setExpanded(nodeRemoved.isExpanded());
    }
    
    @Override
    public void update() {
        tryRemove();
    }
    
    public void tryRemove() {
        if (removeTimer.canDoEvent())
            remove();
    }
    
}

abstract class UpdateNode extends Tree.Node {

    UpdateNode(Actor actor, Object obj) {
        super(actor);
        this.setObject(obj);
    }

    @Override
    public void setExpanded(boolean expanded) {
        if (expanded == isExpanded())
            return;

        super.setExpanded(expanded);
        update();
    }

    public abstract void update();//update children?
    
    
    public void removeAndCreateGhost() {
        setObject(null);//remove reference for GC, TODO: revisit this
        
        new GhostNode(this);
        
        remove();
    }
}
