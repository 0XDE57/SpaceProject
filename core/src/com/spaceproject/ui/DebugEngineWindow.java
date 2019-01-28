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
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.SimpleTimer;

import java.lang.reflect.Field;


public class DebugEngineWindow extends VisWindow implements EntityListener {

    Engine engine;
    Tree tree;
    Node systemNodes, entityNodes;
    Skin skin;

    SimpleTimer refreshRate = new SimpleTimer(2000, true);

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
        if (refreshRate.tryEvent()) {
            System.out.println("refreshNodes!");

            //Array test = new Array();//tree.getNodes();
            //tree.findExpandedObjects(test);
            //tree.clearChildren();


            //systemNodes = new Node(new Label("Systems", skin));
            //systemNodes.removeAll();
            //systemNodes.remove();
            for (EntitySystem sys : engine.getSystems()) {
                int id = sys.hashCode();
                if (!nodeExists(systemNodes, id)) {
                    String sysName = Misc.myToString(sys);
                    Node sysNode = new MyNode(new Label(sysName, skin), id);


                    systemNodes.add(sysNode);
                    //System.out.println("added:" + sysName);
                } else {
                    //System.out.println("node already in tree:" + id);
                }

            }
            //tree.add(systemNodes);



            //entityNodes.removeAll();
            for (Entity ent : engine.getEntities()) {
                addEntityNode(ent);
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
            //tree.add(entityNodes);
            //tree.restoreExpandedObjects(test);
            //tree.expandAll();

        }
    }

    private void addEntityNode(Entity ent) {
        MyNode node = getNode(entityNodes, ent.hashCode());
        if (node == null) {
            MyNode entityNode = new MyNode(new Label(Misc.myToString(ent), skin), ent.hashCode());

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
            
            entityNodes.add(entityNode);
        }
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
        //fadeOut();

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
        switch (keycode) {
            case Input.Keys.UP:
                //tree.getSelection().
                //go to above node
                //
                break;
            case Input.Keys.DOWN:
                //go do below node
                //if node has no siblings at same level, select next node of parent node
                break;
            case Input.Keys.LEFT:
                //if node is expanded, collapse it

                //if node is collaped && node has parent, select parent

                //if node is parent, collapse
                break;
            case Input.Keys.RIGHT:
                //if node has child, expand
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
