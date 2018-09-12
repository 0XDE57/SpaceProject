package com.spaceproject.ui;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import com.spaceproject.screens.MainMenuScreen;

import static com.spaceproject.screens.MyScreenAdapter.game;

/**
 *
 * Modified from https://github.com/kotcrab/vis-editor/blob/master/ui/src/test/java/com/kotcrab/vis/ui/test/manual/TestTabbedPane.java
 */
public class Menu extends VisWindow {
    public final TabbedPane tabbedPane;
    public TestTab customRenderTab, mapTab, placeholderATab, placeholderBTab;


    public Menu (boolean vertical) {
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


        TestTab mainmenu = new TestTab("Menu [ESC]");
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
        mainmenu.add(btnGotoMain);
        mainmenu.add(btnExit);
        tabbedPane.add(mainmenu);


        mapTab = new TestTab("Map [M]");
        tabbedPane.add(mapTab);


        customRenderTab = new TestTab("test render [H]");
        TestShapeRenderActor shapeRenderActor = new TestShapeRenderActor();
        /*getStage().addListener(new InputListener() {
            @Override
            public boolean keyDown (InputEvent event, int keycode) {
                if (keycode == Input.Keys.H) {
                    tabbedPane.switchTab(customRender);
                    return true;
                }

                return false;
            }
        });*/
        customRenderTab.getContentTable().add(shapeRenderActor).grow();
        //TODO: something about the .grow (and also .expand().fill()) is breaking the window resizing
        //customRender.getContentTable().add(new Actor()).grow();
        //customRender.getContentTable().add(new Actor()).expand().fill();
        tabbedPane.add(customRenderTab);

        placeholderATab = new TestTab("placeholder [1]");
        tabbedPane.add(placeholderATab);

        placeholderBTab = new TestTab("placeholder [2]");
        tabbedPane.add(placeholderBTab);

        tabbedPane.switchTab(0);

		//debugAll();
        setSize(Gdx.graphics.getWidth()-150, Gdx.graphics.getHeight()-150);
        centerWindow();
    }



    private class TestTab extends Tab {
        private String title;
        private Table content;

        public TestTab (String title) {
            super(false, false);
            this.title = title;

            content = new VisTable();
            content.setFillParent(true);
        }

        public void add(Actor thing) {
            content.add(thing).growX().row();
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
