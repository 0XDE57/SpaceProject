package com.spaceproject.ui.menu.debug;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Selection;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.menu.debug.nodes.EntityNode;
import com.spaceproject.ui.menu.debug.nodes.GhostNode;
import com.spaceproject.ui.menu.debug.nodes.MyNode;
import com.spaceproject.ui.menu.debug.nodes.ReflectionNode;
import com.spaceproject.ui.menu.debug.nodes.UpdateNode;
import com.spaceproject.utility.SimpleTimer;

import java.util.Arrays;
import java.util.List;

import static com.spaceproject.generation.FontFactory.skinSmallFont;

/**
 * A tree based debug/dev tool for exploring and inspecting the contents of the engine.
 *      view Systems in the Engine, and their Priorities
 *      view Entities
 *      view Components in Entities
 *      view Fields in Components via reflection

 *      todo: view by component, eg:
 *          AIComponent
 *              Entity A
 *              Entity B
 *              Entity C
 *
 *     todo: allow view private fields
 *     todo: allow edit values
 *
 */
public class ECSExplorerWindow extends VisWindow implements EntityListener {
    
    private Engine engine;
    private Tree tree;
    private Node systemNodes, entityNodes;
    
    //todo: move values to config
    private static int refreshRate = 1000;
    public static int newTime = 1000;
    public static int removeTime = 1000;
    public static boolean showHistory = true;
    public static boolean includeChildren = false;
    private SimpleTimer refreshTimer;
    
    private final int keyUP = Input.Keys.UP;
    private final int keyDown = Input.Keys.DOWN;
    private final int keyLeft = Input.Keys.LEFT;
    private final int keyRight = Input.Keys.RIGHT;
    
    
    public ECSExplorerWindow(Engine engine) {
        super("ECS Explorer");
        
        this.engine = engine;
        refreshTimer = new SimpleTimer(refreshRate, true);
        
        
        setResizable(true);
        setMovable(true);
        setSize(400, Gdx.graphics.getHeight() - 20);
        setPosition(10, 10);
        addCloseButton();
        
        
        systemNodes = new MyNode(new Label("Systems", VisUI.getSkin(), skinSmallFont, Color.WHITE));
        entityNodes = new MyNode(new Label("Entities", VisUI.getSkin(), skinSmallFont, Color.WHITE));
        tree = new Tree(VisUI.getSkin());
        tree.add(systemNodes);
        tree.add(entityNodes);
        refreshNodes();
        
        final Table contents = new Table();
        
        final Table options = new Table();
        final CheckBox showHistoryCheck = new CheckBox("show history", VisUI.getSkin());
        showHistoryCheck.setChecked(showHistory);
        showHistoryCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showHistory = showHistoryCheck.isChecked();
            }
        });
        options.add(showHistoryCheck);
        contents.add(options);
        contents.row();
        
        
        final ScrollPane scrollPane = new ScrollPane(tree);
        //scrollPane.setFlickScroll(false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollbarsOnTop(true);
        //scrollPane.scrol
        contents.add(scrollPane).expand().fill();
        add(contents).expand().fill();
        
    }
    
    public void update() {
        if (!isVisible()) {
            return;
        }
        
        if (refreshTimer.tryEvent()) {
            refreshNodes();
        }
    }
    
    private void refreshNodes() {
        if (showHistory) {
            clearGhosts(entityNodes.getChildren());
        }
        updateSystems();
        updateEntities();
    }
    
    private void updateEntities() {
        //update entities
        ((Label)entityNodes.getActor()).setText("Entities [" + engine.getEntities().size() + "]");
        for (Entity entity : engine.getEntities()) {
            Node entNode = entityNodes.findNode(entity);
            if (entNode == null) {
                entityNodes.add(new EntityNode(entity, VisUI.getSkin()));
            } else {
                ((UpdateNode) entNode).update();
            }
        }
    }
    
    private void updateSystems() {
        //add nodes
        ((Label)systemNodes.getActor()).setText("Systems [" + engine.getSystems().size() + "]");
        for (EntitySystem system : engine.getSystems()) {
            Node sysNode = systemNodes.findNode(system);
            if (sysNode == null) {
                systemNodes.add(new ReflectionNode(system));
            }
        }
        
        //update nodes, clean up dead nodes
        for (Object o : systemNodes.getChildren()) {
            UpdateNode node = (UpdateNode)o;
            if (!engine.getSystems().contains((EntitySystem) node.getValue(), false)) {
                node.remove();
            } else {
                node.update();
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
    
    private void show() {
        show(GameScreen.getStage());
    }
    private void show(Stage stage) {
        stage.addActor(this);
        fadeIn();
        
        engine.addEntityListener(this);
        refreshNodes();
    }
    
    private void hide() {
        fadeOut();
        
        engine.removeEntityListener(this);
        systemNodes.clearChildren();
        entityNodes.clearChildren();
    }
    
    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }
    
    public void keyDown(InputEvent event, int keycode) {
        Selection<Node> nodeSelection = tree.getSelection();
        Node selectedNode = nodeSelection.first();
        if (selectedNode == null) {
            List<Integer> keys = Arrays.asList(keyUP, keyDown, keyLeft, keyRight);
            if (keys.contains(keycode)) {
                nodeSelection.choose((Node) tree.getRootNodes().first());
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
                    chosen = (Node) selectedNode.getChildren().first();
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
                Node nextNode = (Node) current.getChildren().get(current.getChildren().size - 1);
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
        entityNodes.add(new EntityNode(entity, VisUI.getSkin(), showHistory));
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        Node node = entityNodes.findNode(entity);
        if (showHistory) {
            ((UpdateNode) node).removeAndCreateGhost(includeChildren);
        } else {
            node.remove();
        }
    }
    
}
