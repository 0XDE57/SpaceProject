package com.spaceproject.ui;


import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.util.value.PrefHeightIfVisibleValue;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
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
 * Modified from https://github.com/kotcrab/vis-editor/blob/master/ui/src/test/java/com/kotcrab/vis/ui/test/manual/TestTabbedPane.java
 */
public class Menu extends VisWindow {
    private final TabbedPane tabbedPane;
    private MyTab mainMenuTab, customRenderTab, mapTab, placeholderATab, placeholderBTab, debugMenuTab, testConfigTab;

    private boolean alwaysHideOnEscape = false;

    public Menu (boolean vertical, Engine engine) {
        super(SpaceProject.TITLE);
        getTitleLabel().setAlignment(Align.center);

        TableUtils.setSpacingDefaults(this);

        setResizable(true);
        setMovable(true);
        setSize(Gdx.graphics.getWidth()-150, Gdx.graphics.getHeight()-150);
        centerWindow();
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
            //force min height for tabbedPane to fix table layout when tab has a VisScrollPane
            //github.com/kotcrab/vis-editor/issues/206#issuecomment-238012673
            add(tabbedPane.getTable()).minHeight(new PrefHeightIfVisibleValue()).growX();
            row();
            add(container).grow();
        }


        mainMenuTab = createMainMenu();
        tabbedPane.add(mainMenuTab);


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


        testConfigTab = createConfigTab();
        tabbedPane.add(testConfigTab);


        tabbedPane.switchTab(mainMenuTab);
    }


    //region menu controls
    public boolean isVisible() {
        return getStage() != null;
    }

    public void hide() {
        fadeOut();
    }

    public void show(Stage stage) {
        stage.addActor(this);
        fadeIn();
    }

    public boolean switchTabForKey(int keycode) {
        //exception for escape key, always hide if shown, or focus main
        if (alwaysHideOnEscape) {
            if (keycode == Input.Keys.ESCAPE) {
                if (isVisible()) {
                    hide();
                } else {
                    tabbedPane.switchTab(mainMenuTab);
                    return true;
                }
                return false;
            }
        }

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
    //endregion


    private MyTab createConfigTab() {
        MyTab test = new MyTab("config", Input.Keys.NUM_3);

        VisTable scrollContainer = new VisTable();
        scrollContainer.left().top();

        for (int i = 0; i < 40; i++) {
            scrollContainer.add(new VisLabel("test: " + i)).row();
        }
        /*
        Config c = SpaceProject.celestcfg;
        try {
            for (Field f : c.getClass().getFields()) {
                Label l = new Label(String.format("%s %s", f.getName(), f.get(c)), VisUI.getSkin());
                scrollContainer.add(l).expand().fill().row();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }*/
        VisScrollPane scrollPane = new VisScrollPane (scrollContainer);
        scrollPane.setFlickScroll(false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(false, false);
        test.getContentTable().add(scrollPane).left().top().expand().fill();
        return test;
    }

    private MyTab createDebugMenu(final Engine engine) {
        MyTab debugTab = new MyTab("Debug", Input.Keys.F4);


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


        final CheckBox togglePos = new CheckBox("show pos", VisUI.getSkin());
        togglePos.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawPos = togglePos.isChecked();
                }
            }
        });


        final CheckBox toggleBounds = new CheckBox("show bounds", VisUI.getSkin());
        toggleBounds.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawBounds = toggleBounds.isChecked();
                }
            }
        });

        final CheckBox toggleOrbitPath = new CheckBox("show orbit path", VisUI.getSkin());
        toggleOrbitPath.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawOrbitPath = toggleOrbitPath.isChecked();
                }
            }
        });


        final CheckBox toggleVectors = new CheckBox("show velocity vectors", VisUI.getSkin());
        toggleVectors.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawVectors = toggleVectors.isChecked();
                }
            }
        });


        final CheckBox toggleMousePos = new CheckBox("show mouse pos", VisUI.getSkin());
        toggleMousePos.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawMousePos = toggleMousePos.isChecked();
                }
            }
        });


        final CheckBox toggleFPS = new CheckBox("show fps", VisUI.getSkin());
        toggleFPS.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawFPS = toggleFPS.isChecked();
                }
            }
        });

        final CheckBox toggleExtraInfo = new CheckBox("show extra info", VisUI.getSkin());
        toggleExtraInfo.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawExtraInfo = toggleExtraInfo.isChecked();
                }
            }
        });

        final CheckBox toggleBoundsPoly = new CheckBox("show bounds poly", VisUI.getSkin());
        toggleBoundsPoly.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawBoundsPoly = toggleBoundsPoly.isChecked();
                }
            }
        });

        final CheckBox toggleEntityList = new CheckBox("show entity list", VisUI.getSkin());
        toggleEntityList.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                DebugUISystem debug = engine.getSystem(DebugUISystem.class);
                if (debug != null) {
                    debug.drawEntityList = toggleEntityList.isChecked();
                }
            }
        });

        Table table = debugTab.getContentTable();
        table.add(toggleFPS).left();
        table.add(toggleExtraInfo).left().row();
        table.add(toggleMousePos).left().row();
        table.add(toggleEntityList).left().row();
        table.add(togglePos).left();
        table.add(toggleComponentList).left().row();
        table.add(toggleBounds).left();
        table.add(toggleBoundsPoly).left().row();
        table.add(toggleVectors).left().row();
        table.add(toggleOrbitPath).left().row();

        return debugTab;
    }

    private MyTab createMainMenu() {
        MyTab menu = new MyTab("Menu", Input.Keys.ESCAPE);


        TextButton btnGotoMain = new TextButton("main menu", VisUI.getSkin());
        btnGotoMain.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                Dialogs.showOptionDialog(getStage(), "Exit", "go to the main menu?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                    @Override
                    public void yes () {
                        game.setScreen(new MainMenuScreen(game));
                    }
                });
            }
        });


        TextButton btnExit = new TextButton("exit", VisUI.getSkin());
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                Dialogs.showOptionDialog(getStage(), "Exit", "close game?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                    @Override
                    public void yes () {
                        Gdx.app.exit();
                    }
                });
            }
        });

        menu.getContentTable().add(btnGotoMain).growX().row();
        menu.getContentTable().add(new TextButton("save", VisUI.getSkin())).growX().row();
        menu.getContentTable().add(new TextButton("load", VisUI.getSkin())).growX().row();
        menu.getContentTable().add(new TextButton("options", VisUI.getSkin())).growX().row();
        menu.getContentTable().add(btnExit).growX().row();
        return menu;
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
