package com.spaceproject.ui;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
        //VisTable container = new VisTable();

        tree = new Tree(skin);


        systemNodes = new Node(new Label("Systems", skin));
        entityNodes = new Node(new Label("Entities", skin));

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
            //System.out.println("refreshNodes!");

            //Array test = new Array();//tree.getNodes();
            //tree.findExpandedObjects(test);
            //tree.clearChildren();


            //systemNodes = new Node(new Label("Systems", skin));
            //systemNodes.removeAll();
            //systemNodes.remove();
            for (EntitySystem system : engine.getSystems()) {
                int id = system.hashCode();
                if (!nodeExists(systemNodes, id)) {
                    String sysName = Misc.myToString(system);
                    Node sysNode = new MyNode(new Label(sysName, skin), id);

                    systemNodes.add(sysNode);
                    //System.out.println("added:" + sysName);
                } else {
                    //System.out.println("node already in tree:" + id);
                }

            }
            //tree.add(systemNodes);



            //entityNodes.removeAll();
            for (Entity entity : engine.getEntities()) {
                MyNode entityNode = addEntityNode(entity);
                MyNode emptyNode = getNode(entityNode, emptyNodeId);
                if (entityNode.isExpanded()) {
                    System.out.println("updating entity: " + entityNode.getId());

                    //update components
                    for (Component component : entity.getComponents()) {
                        MyNode compNode = getNode(entityNode, component.hashCode());
                        if (compNode == null) {
                            compNode = new MyNode(new Label(Misc.myToString(component), skin), component.hashCode());
                            entityNode.add(compNode);
                        } else {
                            compNode.removeAll();
                        }

                        //update fields
                        for (Field f : component.getClass().getFields()) {
                            try {
                                String field = String.format("\t\t%-14s %s", f.getName(), f.get(component));
                                Node fieldNode = new Node(new Label(field, skin));
                                compNode.add(fieldNode);
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    //clean dead components
                    if (entity.getComponents().size() != entityNode.getChildren().size) {
                        for (Node node : entityNode.getChildren()) {
                            boolean foundNode = false;
                            for (Component component : entity.getComponents()) {
                                if (((MyNode) node).getId() == component.hashCode()) {
                                    foundNode = true;
                                    break;
                                }
                            }
                            if (!foundNode) {
                                node.remove();
                            }
                        }
                    }


                    if (emptyNode != null) {
                        emptyNode.remove();
                        entityNode.setExpanded(true);
                    }

                } else {

                    if (emptyNode == null) {
                        entityNode.removeAll();
                        entityNode.add(new MyNode(new Label("temp node", skin), emptyNodeId));
                        System.out.println("node reset will temp node: " + entityNode.getId());
                    }
                }
                //String entName = Misc.myToString(ent);//.getClass().getSimpleName();
                //Node entNode = new Node(new Label(entName, skin));

                /*
                for (Component comp : ent.getComponents()) {
                    Node compNode = new Node(new Label(Misc.myToString(comp), skin));
                    entNode.add(compNode);
                    /*
                    System.out.println("\t" + c.toString());
                    for (Field f : c.getClass().getFields()) {
                        try {
                            System.out.println(String.format("\t\t%-14s %s", f.getName(), f.get(c)));
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }

                }*/


            }
/*
            for (Node entNode : entityNodes.getChildren()) {
                if (entNode.isExpanded()) {
                    entNode.removeAll();

                    for (Component comp : ent.getComponents()) {
                        MyNode compNode = getNode(entityNode, comp.hashCode());
                        if (compNode == null) {
                            compNode = new MyNode(new Label(Misc.myToString(comp), skin), comp.hashCode());

                            for (Field f : comp.getClass().getFields()) {
                                try {
                                    String field = String.format("\t\t%-14s %s", f.getName(), f.get(comp));
                                    Node fieldNode = new Node(new Label(field, skin));
                                    compNode.add(fieldNode);
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            entityNode.add(compNode);
                        }

                    }

                } else {
                    MyNode emptyNode = getNode(entNode, emptyNodeId);
                    if (emptyNode == null) {
                        entNode.removeAll();
                        entNode.add(new MyNode(new Label("temp node", skin), emptyNodeId));
                    }
                }
            }*/
            //tree.add(entityNodes);
            //tree.restoreExpandedObjects(test);
            //tree.expandAll();

        }
    }

    private final int emptyNodeId = -1;

    private MyNode addEntityNode(Entity ent) {
        MyNode node = getNode(entityNodes, ent.hashCode());
        if (node == null) {
            node = new MyNode(new Label(Misc.myToString(ent), skin), ent.hashCode());

            node.add(new MyNode(new Label("temp node", skin), emptyNodeId));
            /*
            for (Component comp : ent.getComponents()) {
                MyNode compNode = getNode(entityNode, comp.hashCode());
                if (compNode == null) {
                    compNode = new MyNode(new Label(Misc.myToString(comp), skin), comp.hashCode());

                    for (Field f : comp.getClass().getFields()) {
                        try {
                            String field = String.format("\t\t%-14s %s", f.getName(), f.get(comp));
                            Node fieldNode = new Node(new Label(field, skin));
                            compNode.add(fieldNode);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    entityNode.add(compNode);
                }

            }*/

            entityNodes.add(node);
        }

        return node;
    }

    private boolean nodeExists(Node parentNode, long id) {
        return getNode(parentNode, id) != null;
    }

    private MyNode getNode(Node parentNode, long id) {
        for (Node node : parentNode.getChildren()) {
            if (node instanceof MyNode) {
                if (((MyNode)node).getId() == id) {
                    return (MyNode)node;
                }
            }
        }

        return null;
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
        //tree.clearChildren();
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
            List<Integer> keys = Arrays.asList(Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT);
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
            case Input.Keys.UP:
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
            case Input.Keys.DOWN:
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
            case Input.Keys.LEFT:
                if (selectedNode.isExpanded()) {
                    selectedNode.setExpanded(false);
                } else {
                    if (parentNode != null) {
                        nodeSelection.choose(parentNode);
                    }
                }
                break;
            case Input.Keys.RIGHT:
                if (!selectedNode.isExpanded()) {
                    selectedNode.setExpanded(true);
                }
                break;
            case Input.Keys.SPACE:
                refreshNodes();
                break;
        }
    }


    @Override
    public void entityAdded(Entity entity) {
        addEntityNode(entity);
    }

    @Override
    public void entityRemoved(Entity entity) {
        MyNode node = getNode(entityNodes, entity.hashCode());
        if (node != null) {
            tree.remove(node);
        }
    }


}
