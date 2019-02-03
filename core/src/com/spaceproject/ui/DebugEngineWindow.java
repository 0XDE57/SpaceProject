package com.spaceproject.ui;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

    private final int emptyNodeId = -1;

    final int keyUP = Input.Keys.UP;
    final int keyDown = Input.Keys.DOWN;
    final int keyLeft = Input.Keys.LEFT;
    final int keyRight = Input.Keys.RIGHT;

    public DebugEngineWindow(Engine engine) {
        super("Debug Engine View");

        this.engine = engine;


        setResizable(true);
        setMovable(true);
        setSize(Gdx.graphics.getWidth()-150, Gdx.graphics.getHeight()-150);
        //setPosition(0,0, Align.topLeft);
        centerWindow();
        addCloseButton();


        TableUtils.setSpacingDefaults(this);
        columnDefaults(0).left();



        skin = VisUI.getSkin();
        
        systemNodes = new Node(new Label("Systems", skin));
        entityNodes = new Node(new Label("Entities", skin));
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

            for (EntitySystem system : engine.getSystems()) {
                Node sysNode = systemNodes.findNode(system);
                if (sysNode == null) {
                    systemNodes.add(new ReflectionNode(system));
                } else {
                    ((ReflectionNode)sysNode).update();
                }
            }



            for (Entity entity : engine.getEntities()) {
                Node entNode = entityNodes.findNode(entity);
                if (entNode == null) {
                    entityNodes.add(new EntityNode(entity, skin));
                } else {
                    ((EntityNode)entNode).update();
                }
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

        switch (keycode) {
            case keyUP:
                if (index > 0) {
                    Node upNode = siblingNodes.get(index - 1);
                    if (upNode.isExpanded()) {
                        
                        Node nextNode = upNode.getChildren().get(upNode.getChildren().size-1);
                        nodeSelection.choose(nextNode);
                    } else {
                        nodeSelection.choose(upNode);
                    }
                } else {
                    if (parentNode != null) {
                        nodeSelection.choose(parentNode);
                    }
                }
                break;
            case keyDown:
                if (selectedNode.isExpanded()) {
                    if (selectedNode.getChildren() != null && selectedNode.getChildren().size > 0) {
                        Node firstChild = selectedNode.getChildren().first();
                        if (firstChild != null) {
                            nodeSelection.choose(firstChild);
                        }
                    }
                } else {
                    if (index < siblingNodes.size - 1) {
                        Node downNode = siblingNodes.get(index + 1);
                        nodeSelection.choose(downNode);
                    } else if (index == siblingNodes.size - 1) {
                        if (parentNode != null) {
                            Node parentsParent = parentNode.getParent(); //(grandparents?)
                            if (parentsParent != null) {
                                final Array<Node> parentsSiblings = parentsParent.getChildren();//(aunts/uncles?)
                                index = parentsParent.getChildren().indexOf(parentNode, false);
                                if (index < parentsSiblings.size - 1) {
                                    Node downNode = parentsSiblings.get(index + 1);
                                    nodeSelection.choose(downNode);
                                }
                            }
                        }
                    }
                }
                break;
            case keyLeft:
                if (selectedNode.isExpanded()) {
                    selectedNode.setExpanded(false);
                } else {
                    if (parentNode != null) {
                        nodeSelection.choose(parentNode);
                    }
                }
                break;
            case keyRight:
                if (!selectedNode.isExpanded()) {
                    selectedNode.setExpanded(true);
                }
                break;
        }
    }
    //endregion

    @Override
    public void entityAdded(Entity entity) {
        entityNodes.add(new EntityNode(entity, skin));
    }

    @Override
    public void entityRemoved(Entity entity) {
        entityNodes.findNode(entity).remove();
    }

}

class EntityNode extends UpdateNode {

    public EntityNode(Entity entity, Skin skin) {
        super(new Label("init", skin), entity);
    }

    public Entity getEntity() {
        return (Entity) getObject();
    }
    
    @Override
    public void update() {
        ((Label)getActor()).setText(toString());
        
        if (!isExpanded())
            return;

        //update nodes
        ImmutableArray<Component> components = getEntity().getComponents();
        for (Component comp : components) {
            if (findNode(comp) == null) {
                add(new ReflectionNode(comp));
            }
        }

        //clean up dead nodes
        for (Node node : getChildren()) {
            if (!components.contains((Component)node.getObject(),false)) {
                node.remove();
            }
            ((ReflectionNode)node).update();
        }
    }
    
    @Override
    public String toString() {
        return Misc.myToString(getEntity()) + " [" + getEntity().getComponents().size() + "]";
    }
}

class ReflectionNode extends UpdateNode {

    public ReflectionNode(Object object) {
        super(new Label(Misc.myToString(object), VisUI.getSkin()), object);
        init();
    }
    
    private void init() {
        for (Field f : getObject().getClass().getFields()) {
            add(new FieldNode(new Label("init", VisUI.getSkin()), getObject(), f));
        }
    }

    @Override
    public void update() {
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

    public abstract void update();
}
