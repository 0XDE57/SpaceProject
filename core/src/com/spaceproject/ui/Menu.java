package com.spaceproject.ui;


import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.util.value.PrefHeightIfVisibleValue;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel;
import com.kotcrab.vis.ui.widget.spinner.SimpleFloatSpinnerModel;
import com.kotcrab.vis.ui.widget.spinner.Spinner;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.Config;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.systems.DebugSystem;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import static com.spaceproject.screens.MyScreenAdapter.game;

/**
 * Modified from https://github.com/kotcrab/vis-editor/blob/master/ui/src/test/java/com/kotcrab/vis/ui/test/manual/TestTabbedPane.java
 */
public class Menu extends VisWindow {
    private final TabbedPane tabbedPane;
    private Tab mainMenuTab, customRenderTab, mapTab, placeholderATab, placeholderBTab, debugMenuTab, testConfigTab, keyConfigTab;
    
    private boolean alwaysHideOnEscape = false;
    
    public Menu(boolean vertical, Engine engine) {
        super(SpaceProject.TITLE + "    (" + SpaceProject.VERSION + ")");
        getTitleLabel().setAlignment(Align.center);
        
        TableUtils.setSpacingDefaults(this);
        
        setResizable(true);
        setMovable(true);
        setSize(Gdx.graphics.getWidth() - 150, Gdx.graphics.getHeight() - 150);
        centerWindow();
        addCloseButton();
        
        final VisTable container = new VisTable();
        
        TabbedPane.TabbedPaneStyle style = VisUI.getSkin().get(vertical ? "vertical" : "default", TabbedPane.TabbedPaneStyle.class);
        tabbedPane = new TabbedPane(style);
        tabbedPane.addListener(new TabbedPaneAdapter() {
            @Override
            public void switchedTab(Tab tab) {
                //Gdx.app.log(this.getClass().getSimpleName(), "TabbedPaneAdapter switched tab: " + tab.getTabTitle());
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
        
        
        mapTab = new HotKeyTab("map", Input.Keys.M);
        tabbedPane.add(mapTab);
        
        
        customRenderTab = new HotKeyTab("test render", Input.Keys.H);
        TestShapeRenderActor shapeRenderActor = new TestShapeRenderActor();
        
        customRenderTab.getContentTable().add(shapeRenderActor).grow();
        //TODO: something about the .grow (and also .expand().fill()) is breaking the window resizing
        //customRender.getContentTable().add(new Actor()).grow();
        //customRender.getContentTable().add(new Actor()).expand().fill();
        tabbedPane.add(customRenderTab);
        
        
        placeholderATab = new HotKeyTab("placeholder", Input.Keys.NUM_1);
        placeholderATab.getContentTable().add(new TextButton("do stuff", VisUI.getSkin()));
        tabbedPane.add(placeholderATab);
        
        placeholderBTab = new HotKeyTab("placeholder", Input.Keys.NUM_2);
        //test rainbow text
        BitmapFont font = VisUI.getSkin().get("default-font", BitmapFont.class);
        font.getData().markupEnabled = true;
        Label testRainbowLabel = new Label("<<[BLUE]M[RED]u[YELLOW]l[GREEN]t[OLIVE]ic[]o[]l[]o[]r[]*[MAROON]Label[][] [Unknown Color]>>", VisUI.getSkin());
        placeholderBTab.getContentTable().add(testRainbowLabel);
        placeholderBTab.getContentTable().row();
        placeholderBTab.getContentTable().add(new Label("[RED]This[BLUE] is a [GREEN]test!", VisUI.getSkin()));
        tabbedPane.add(placeholderBTab);
        
        
        debugMenuTab = createDebugMenu(engine);
        tabbedPane.add(debugMenuTab);
        
        
        testConfigTab = createConfigTab(SpaceProject.celestCFG);
        tabbedPane.add(testConfigTab);
        
        keyConfigTab = new KeyConfigTab("Input Settings", SpaceProject.keyCFG);
        //tabbedPane.add(keyConfigTab);
        
        //tabbedPane.add(createConfigTab(new TestConfig()));
        
        tabbedPane.switchTab(mainMenuTab);
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
        Gdx.app.log(this.getClass().getSimpleName(), "menu hide");
        /*
        ConfigTab tab = checkTabChanges();
        if (tab != null) {
            tab.promptSaveChanges();
            return;
        }
        */
        
        fadeOut();
    }
    
    @Override
    protected void close() {
        Gdx.app.log(this.getClass().getSimpleName(), "menu close");
        /*
        ConfigTab tab = checkTabChanges();
        if (tab != null) {
            tab.promptSaveChanges();
            return;
        }
        */
        
        super.close();
    }
    
    private ConfigTab checkTabChanges() {
        for (final Tab tab : tabbedPane.getTabs()) {
            if (tab instanceof ConfigTab) {
                if (((ConfigTab) tab).isDirty()) {
                    return (ConfigTab) tab;
                }
            }
        }
        return null;
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
            if (tab instanceof HotKeyTab) {
                if (((HotKeyTab) tab).getHotKey() == keycode) {
                    if (tabbedPane.getActiveTab().equals(tab) && getStage() != null) {
                        fadeOut();
                        return false;
                    }
                    tabbedPane.switchTab(tab);
                    return true;
                    
                }
            }
        }
        return false;
    }
    //endregion
    
    
    private HotKeyTab createConfigTab(Config config) {
        HotKeyTab test = new HotKeyTab(config.getClass().getSimpleName(), Input.Keys.NUM_3);
        int padSize = 2;
        
        final VisTable scrollContainer = new VisTable();
        scrollContainer.left().top();
        
        
        try {
            for (Field f : config.getClass().getFields()) {
                String fieldName = f.getName();
                Type t = f.getType();
                Actor a;
                
                if (t == Integer.TYPE) {
                    a = new Spinner(fieldName, new IntSpinnerModel((Integer) f.get(config), Integer.MIN_VALUE, Integer.MAX_VALUE));
                } else if (t == Float.TYPE) {
                    a = new Spinner(fieldName, new SimpleFloatSpinnerModel((Float) f.get(config), -Float.MAX_VALUE, Float.MAX_VALUE, 0.1f, 4));
                } else if (t == Boolean.TYPE) {
                    a = new CheckBox(fieldName, VisUI.getSkin());
                    ((CheckBox) a).setChecked((Boolean) f.get(config));
                } else {
                    a = new Label(String.format("%s %s", fieldName, f.get(config)), VisUI.getSkin());
                }
                
                if (a != null) {
                    scrollContainer.add(a).expand().fill().pad(padSize).row();
                } else {
                    scrollContainer.add(new Label("Failed to reflect field: " + fieldName, VisUI.getSkin()));
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            scrollContainer.add(new Label("Failed to reflect field: " + e.getMessage(), VisUI.getSkin()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            scrollContainer.add(new Label("Failed to reflect field: " + e.getMessage(), VisUI.getSkin()));
        }
        //}
        
        final VisScrollPane scrollPane = new VisScrollPane(scrollContainer);
        scrollPane.addListener(new EventListener() {
            //auto focus scroll on mouse enter
            @Override
            public boolean handle(Event event) {
                if (event instanceof InputEvent)
                    if (((InputEvent) event).getType() == InputEvent.Type.enter)
                        event.getStage().setScrollFocus(scrollPane);
                return false;
            }
        });
        scrollPane.setFlickScroll(false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(false, false);
        test.getContentTable().add(scrollPane).left().top().expand().fill();
        return test;
    }
    
    private HotKeyTab createDebugMenu(final Engine engine) {
        HotKeyTab debugTab = new HotKeyTab("Debug", Input.Keys.F4);
        
        
        final CheckBox toggleComponentList = new CheckBox("show components", VisUI.getSkin());
        toggleComponentList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
                if (debug != null) {
                    debug.drawComponentList = toggleComponentList.isChecked();
                }
            }
        });
        
        
        final CheckBox togglePos = new CheckBox("show pos", VisUI.getSkin());
        togglePos.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
                if (debug != null) {
                    debug.drawPos = togglePos.isChecked();
                }
            }
        });
        
        
        final CheckBox toggleBounds = new CheckBox("show box2d debug", VisUI.getSkin());
        toggleBounds.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
                if (debug != null) {
                    debug.box2DDebugRender = toggleBounds.isChecked();
                }
            }
        });
        
        final CheckBox toggleOrbitPath = new CheckBox("show orbit path", VisUI.getSkin());
        toggleOrbitPath.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
                if (debug != null) {
                    debug.drawOrbitPath = toggleOrbitPath.isChecked();
                }
            }
        });
        
        
        final CheckBox toggleVectors = new CheckBox("show velocity vectors", VisUI.getSkin());
        toggleVectors.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
                if (debug != null) {
                    debug.drawVectors = toggleVectors.isChecked();
                }
            }
        });
        
        
        final CheckBox toggleMousePos = new CheckBox("show mouse pos", VisUI.getSkin());
        toggleMousePos.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
                if (debug != null) {
                    debug.drawMousePos = toggleMousePos.isChecked();
                }
            }
        });
        
        
        final CheckBox toggleFPS = new CheckBox("show fps", VisUI.getSkin());
        toggleFPS.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
                if (debug != null) {
                    debug.drawFPS = toggleFPS.isChecked();
                }
            }
        });
        
        final CheckBox toggleExtraInfo = new CheckBox("show extra info", VisUI.getSkin());
        toggleExtraInfo.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
                if (debug != null) {
                    debug.drawExtraInfo = toggleExtraInfo.isChecked();
                }
            }
        });
        
        final CheckBox toggleEntityList = new CheckBox("show entity list", VisUI.getSkin());
        toggleEntityList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DebugSystem debug = engine.getSystem(DebugSystem.class);
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
        table.add(toggleBounds).left().row();
        table.add(toggleVectors).left().row();
        table.add(toggleOrbitPath).left().row();
        
        return debugTab;
    }
    
    private HotKeyTab createMainMenu() {
        HotKeyTab menu = new HotKeyTab("Menu", Input.Keys.ESCAPE);
        
        
        TextButton btnGotoMain = new TextButton("main menu", VisUI.getSkin());
        btnGotoMain.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOptionDialog(getStage(), "Exit", "go to the main menu?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        game.setScreen(new TitleScreen(game));
                    }
                });
            }
        });
        
        
        TextButton btnExit = new TextButton("exit", VisUI.getSkin());
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOptionDialog(getStage(), "Exit", "close game?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        Gdx.app.exit();
                    }
                });
            }
        });
        
        TextButton btnOptions = new TextButton("options", VisUI.getSkin());
        btnOptions.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!tabbedPane.getTabs().contains(keyConfigTab, false)) {
                    tabbedPane.add(keyConfigTab);
                }
                tabbedPane.switchTab(keyConfigTab);
            }
        });
        
        menu.getContentTable().add(btnGotoMain).growX().pad(2).row();
        menu.getContentTable().add(new TextButton("save", VisUI.getSkin())).growX().pad(2).row();
        menu.getContentTable().add(new TextButton("load", VisUI.getSkin())).growX().pad(2).row();
        menu.getContentTable().add(btnOptions).growX().pad(2).row();
        menu.getContentTable().add(btnExit).growX().pad(2).row();
        return menu;
    }
    
    
}
