package com.spaceproject.ui;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
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


public class DebugEngineWindow extends VisWindow {

    Engine engine;
    Tree tree;
    Node systemNodes, entityNodes;
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



        Skin skin = VisUI.getSkin();
        //VisTable container = new VisTable();

        tree = new Tree(skin);





        /*
        Node item1 = new Node(new Label("item 1", skin));
        Node item2 = new Node(new Label("item 2", skin));
        Node item3 = new Node(new Label("item 3", skin));

        item1.add(new Node(new Label("item 1.1", skin)));
        item1.add(new Node(new Label("item 1.2", skin)));
        item1.add(new Node(new Label("item 1.3", skin)));

        item2.add(new Node(new Label("item 2.1", skin)));
        item2.add(new Node(new Label("item 2.2", skin)));
        item2.add(new Node(new Label("item 2.3", skin)));

        item3.add(new Node(new Label("item 3.1", skin)));
        item3.add(new Node(new Label("item 3.2", skin)));
        item3.add(new Node(new Label("item 3.3", skin)));


        item1.setExpanded(true);

        tree.add(item1);
        tree.add(item2);
        tree.add(item3);*/
        systemNodes = new Node(new Label("Systems", skin));
        entityNodes = new Node(new Label("Entities", skin));
        refreshNodes();


        ScrollPane scrollPane = new ScrollPane(tree);
        //scrollPane.setFadeScrollBars(true);
        //scrollPane.setScrollbarsOnTop(true);
        //scrollPane.scrol
        add(scrollPane).expand().fill();

    }

    public void refreshNodes() {
        if (refreshRate.tryEvent()) {
            System.out.println("refreshNodes!");

            //Array test = new Array();//tree.getNodes();
            //tree.findExpandedObjects(test);
            tree.clearChildren();

            Skin skin = VisUI.getSkin();
            //systemNodes = new Node(new Label("Systems", skin));
            systemNodes.removeAll();
            //systemNodes.remove();
            for (EntitySystem sys : engine.getSystems()) {
                String sysName = Misc.myToString(sys);//.getClass().getSimpleName() + "" + sys.getClass().hashCode();
                Node sysNode = new Node(new Label(sysName, skin));
                systemNodes.add(sysNode);
            }
            tree.add(systemNodes);

            //Node entityNodes = new Node(new Label("Entities", skin));
            //entityNodes.getChildren().clear();
            entityNodes.removeAll();
            for (Entity ent : engine.getEntities()) {
                String entName = Misc.myToString(ent);//.getClass().getSimpleName();
                Node entNode = new Node(new Label(entName, skin));
                entityNodes.add(entNode);
            }
            tree.add(entityNodes);
            //tree.restoreExpandedObjects(test);
            //tree.expandAll();

        }
    }

    //region menu controls
    public boolean isVisible() {
        return getStage() != null;
    }

    public void show(Stage stage) {
        stage.addActor(this);
        fadeIn();
    }

    public void hide() {
        fadeOut();
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


}
