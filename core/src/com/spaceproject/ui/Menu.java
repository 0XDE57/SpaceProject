package com.spaceproject.ui;


import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;
import com.spaceproject.SpaceProject;
import com.spaceproject.screens.MainMenuScreen;
import com.spaceproject.systems.DebugUISystem;

import static com.spaceproject.screens.MyScreenAdapter.game;

/**
 *
 * Modified from https://github.com/kotcrab/vis-editor/blob/master/ui/src/test/java/com/kotcrab/vis/ui/test/manual/TestTabbedPane.java
 */
public class Menu extends VisWindow {
    public final TabbedPane tabbedPane;
    public MyTab mainmenu, customRenderTab, mapTab, placeholderATab, placeholderBTab, debugMenuTab;


    public Menu (boolean vertical, Engine engine) {
        super("a space project");

        TableUtils.setSpacingDefaults(this);

        setResizable(true);
        addCloseButton();

        final VisTable container = new VisTable();

        TabbedPane.TabbedPaneStyle style = VisUI.getSkin().get(vertical ? "vertical" : "default", TabbedPane.TabbedPaneStyle.class);
        tabbedPane = new TabbedPane(style);
        tabbedPane.addListener(new TabbedPaneAdapter() {
            @Override
            public void switchedTab (Tab tab) {
                container.clearChildren();
                container.add(tab.getContentTable()).expand().fill();
            }
        });

        if (style.vertical) {
            top();
            defaults().top();
            add(tabbedPane.getTable()).growY();
            add(container).expand().fill();
        } else {
            add(tabbedPane.getTable()).expandX().fillX();
            row();
            add(container).expand().fill();
        }


        mainmenu = createMainMenu();
        tabbedPane.add(mainmenu);


        mapTab = new MyTab("map", Input.Keys.M);
        tabbedPane.add(mapTab);


        customRenderTab = new MyTab("test render", Input.Keys.H);
        TestShapeRenderActor shapeRenderActor = new TestShapeRenderActor();

        customRenderTab.getContentTable().add(shapeRenderActor).grow();
        //TODO: something about the .grow (and also .expand().fill()) is breaking the window resizing
        //customRender.getContentTable().add(new Actor()).grow();
        //customRender.getContentTable().add(new Actor()).expand().fill();
        tabbedPane.add(customRenderTab);



        placeholderATab = new MyTab("placeholder", Input.Keys.NUM_1);
        placeholderATab.getContentTable().add(new TextButton("do stuff", VisUI.getSkin()));
        tabbedPane.add(placeholderATab);

        placeholderBTab = new MyTab("placeholder", Input.Keys.NUM_2);
        tabbedPane.add(placeholderBTab);


        debugMenuTab = createDebugMenu(engine);
        tabbedPane.add(debugMenuTab);

        tabbedPane.switchTab(mainmenu);

		//debugAll();
        setSize(Gdx.graphics.getWidth()-150, Gdx.graphics.getHeight()-150);
        centerWindow();
    }

    private MyTab createDebugMenu(final Engine engine) {
        MyTab debugTab = new MyTab("Debug", SpaceProject.keycfg.toggleDebug);
        final CheckBox toggleComponentList = new CheckBox("show components", VisUI.getSkin());
        toggleComponentList.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawComponentList = toggleComponentList.isChecked();
                }
            }
        });
        debugTab.getContentTable().add(toggleComponentList);
        return debugTab;
    }

    private MyTab createMainMenu() {
        MyTab menu = new MyTab("Menu", Input.Keys.ESCAPE);
        TextButton btnGotoMain = new TextButton("main menu", VisUI.getSkin());
        btnGotoMain.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        TextButton btnExit = new TextButton("exit", VisUI.getSkin());
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        menu.getContentTable().add(btnGotoMain).growX().row();
        menu.getContentTable().add(btnExit).growX().row();
        return menu;
    }

    public boolean switchTabForKey(int keycode) {
        /* //exception for escape key, always hide if shown, or focus main
        if (keycode == mainmenu.hotKey) {
            if (getStage() != null) {
                fadeOut();
            } else {
                tabbedPane.switchTab(mainmenu);
                return true;
            }
            return false;
        }*/

        for (Tab tab : tabbedPane.getTabs()) {
            if (((MyTab)tab).getHotKey() == keycode) {
                if (tabbedPane.getActiveTab().equals(tab) && getStage() != null) {
                    fadeOut();
                    return false;
                }
                tabbedPane.switchTab(tab);
                return true;

            }
        }
        return false;
    }


    private class MyTab extends Tab {
        private String title;
        private Table content;
        private int hotKey;

        public MyTab(String title, int hotKey) {
            super(false, false);
            this.hotKey = hotKey;
            this.title = title + " [" + Input.Keys.toString(hotKey) + "]";

            content = new VisTable();
            content.setFillParent(true);

        }

        public int getHotKey() {
            return hotKey;
        }

        @Override
        public String getTabTitle () {
            return title;
        }

        @Override
        public Table getContentTable () {
            return content;
        }
    }
}
