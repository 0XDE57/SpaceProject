package com.spaceproject.ui.menu;


import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.SnapshotArray;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.util.value.PrefHeightIfVisibleValue;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.ui.ShapeRenderActor;
import com.spaceproject.ui.menu.tabs.ConfigTab;
import com.spaceproject.ui.menu.tabs.DebugTab;
import com.spaceproject.ui.menu.tabs.HotKeyTab;
import com.spaceproject.ui.menu.tabs.KeyConfigTab;
import com.spaceproject.ui.menu.tabs.MainMenuTab;
import com.spaceproject.ui.menu.tabs.MyTab;
import com.spaceproject.utility.IndependentTimer;


public class GameMenu extends VisWindow implements ControllerListener {
    
    private GameScreen game;
    
    private final TabbedPane tabbedPane;
    private final VisTable container;
    private final Tab mainMenuTab;
    private final Tab keyConfigTab;
    
    private boolean pauseOnMenuOpen = true;
    private boolean alwaysHideOnEscape = false;
    private boolean retainPositionOnOpen = true;
    private boolean isResizable = true;
    private boolean isMovable = true;
    private float ratio = 0.66f;//scaling, how much screen does menu cover
    private boolean debugShowPlaceholderTests = true;
    private int focusIndex = -1;
    private float leftStickVertAxis;
    private IndependentTimer lastFocusTimer = new IndependentTimer(200, true);
    
    public GameMenu(GameScreen game, boolean vertical) {
        super(SpaceProject.TITLE + " (" + SpaceProject.VERSION + ")");
        getTitleLabel().setAlignment(Align.center);
        
        this.game = game;
        
        TableUtils.setSpacingDefaults(this);
        setResizable(isResizable);
        setMovable(isMovable);
        addCloseButton();
        container = new VisTable();
        
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

        //add tabs
        mainMenuTab = new MainMenuTab(this);
        tabbedPane.add(mainMenuTab);
        
        keyConfigTab = new KeyConfigTab(getStage(), SpaceProject.configManager.getConfig(KeyConfig.class), "Input Settings");
        //keyConfigTab = new ConfigTab(this, SpaceProject.configManager.getConfig(KeyConfig.class));
        //tabbedPane.add(keyConfigTab);
        
        tabbedPane.add(new DebugTab());
        
        if (debugShowPlaceholderTests) {
            addTestTabs();
        }
        
        tabbedPane.switchTab(mainMenuTab);

        game.getStage().addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                if (isVisible()) {
                    game.getEngine().getSystem(DesktopInputSystem.class).setFocusToDesktop();
                    removeFocus(game.getStage());
                }
                return super.mouseMoved(event, x, y);
            }
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (!isVisible()) {
                    return super.keyDown(event, keycode);
                }
                game.getEngine().getSystem(DesktopInputSystem.class).setFocusToDesktop();
                switch (keycode) {
                    case Input.Keys.W:
                    case Input.Keys.UP:
                        return updateFocus(game.getStage(), true);
                    case Input.Keys.S:
                    case Input.Keys.DOWN:
                        return updateFocus(game.getStage(), false);
                    case Input.Keys.SPACE:
                    case Input.Keys.ENTER:
                        return selectFocusedActor();
                }
                return super.keyDown(event, keycode);
            }
        });
    }

    private boolean updateFocus(Stage stage, boolean up) {
        SnapshotArray<Actor> children = tabbedPane.getActiveTab().getContentTable().getChildren();
        if (children.size == 0) return false;

        if (up) {
            focusIndex--;
        } else {
            focusIndex++;
        }
        focusIndex = focusIndex % children.size;
        if (focusIndex < 0) {
            focusIndex = children.size - 1;
        }

        stage.setKeyboardFocus(children.get(focusIndex));
        return true;
    }

    private boolean removeFocus(Stage stage) {
        focusIndex = -1;
        stage.setKeyboardFocus(null);
        return true;
    }

    private boolean selectFocusedActor() {
        if (focusIndex == -1) return false;

        Actor currentActor = tabbedPane.getActiveTab().getContentTable().getChildren().get(focusIndex);
        InputEvent touchEvent = new InputEvent();
        touchEvent.setType(InputEvent.Type.touchDown);
        currentActor.fire(touchEvent);

        touchEvent = new InputEvent();
        touchEvent.setType(InputEvent.Type.touchUp);
        currentActor.fire(touchEvent);
        return true;
    }
    
    private void addTestTabs() {
        Tab customRenderTab = new MyTab("Debug spectrum render");
        ShapeRenderActor shapeRenderActor = new ShapeRenderActor();
        customRenderTab.getContentTable().add(shapeRenderActor).grow();
        customRenderTab.getContentTable().row();
        Slider sliderGamma = new Slider(0, 1, 0.1f, false,VisUI.getSkin());
        customRenderTab.getContentTable().add(sliderGamma);
        //TODO: something about the .grow (and also .expand().fill()) is breaking the window resizing
        //customRender.getContentTable().add(new Actor()).grow();
        //customRender.getContentTable().add(new Actor()).expand().fill();
        tabbedPane.add(customRenderTab);
        
        /*
        Tab placeholderBTab = new MyTab("Color Text Test");
        //test rainbow text
        BitmapFont font = VisUI.getSkin().get("default-font", BitmapFont.class);
        font.getData().markupEnabled = true;
        Label testRainbowLabel = new Label("<<[BLUE]M[RED]u[YELLOW]l[GREEN]t[OLIVE]ic[]o[]l[]o[]r[]*[MAROON]Label[][] [Unknown Color]>>", VisUI.getSkin());
        placeholderBTab.getContentTable().add(testRainbowLabel);
        placeholderBTab.getContentTable().row();
        placeholderBTab.getContentTable().add(new Label("[RED]This[BLUE] is a [GREEN]test!", VisUI.getSkin()));
        tabbedPane.add(placeholderBTab);
        */
    }
    
    //region menu controls
    public boolean isVisible() {
        return getStage() != null;
    }
    
    public void show() {
        show(GameScreen.getStage());
    }
    
    private void show(Stage stage) {
        stage.addActor(this);
        
        if (!retainPositionOnOpen) {
            resetPosition();
        }
        fadeIn();
    
        if (pauseOnMenuOpen) {
            game.pause();
        }
    }
    
    public void resetPosition() {
        setSize(Gdx.graphics.getWidth() * ratio, Gdx.graphics.getHeight() * ratio);
        
        if (Gdx.graphics.getWidth() < 400) {
            setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        
        centerWindow();
    }
    
    @Override
    public void close() {
        Gdx.app.log(getClass().getSimpleName(), "menu close");
        /*
        ConfigTab tab = checkTabChanges();
        if (tab != null) {
            tab.promptSaveChanges();
            return;
        }*/
        
        game.resume();
        
        fadeOut();
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
                    close();
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
                        close();
                        return false;
                    }
                    tabbedPane.switchTab(tab);
                    return true;
                }
            }
        }
        return false;
    }
    
    public TabbedPane getTabbedPane() {
        return tabbedPane;
    }
    //endregion
    
    public Tab getKeyConfigTab() {
        return keyConfigTab;
    }

    @Override
    public void connected(Controller controller) {}

    @Override
    public void disconnected(Controller controller) {}

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        game.getEngine().getSystem(DesktopInputSystem.class).setFocusToController();
        if (buttonCode == controller.getMapping().buttonDpadUp) {
            return updateFocus(game.getStage(),true);
        }
        if (buttonCode == controller.getMapping().buttonDpadDown) {
            return updateFocus(game.getStage(),false);
        }
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        if (buttonCode == controller.getMapping().buttonA) {
            return selectFocusedActor();
        }
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        game.getEngine().getSystem(DesktopInputSystem.class).setFocusToController();
        if (axisCode == controller.getMapping().axisLeftY) {
            leftStickVertAxis = value;
        }
        if (Math.abs(leftStickVertAxis) > 0.6f && lastFocusTimer.tryEvent()) {
            if (leftStickVertAxis > 0) {
                return updateFocus(game.getStage(),false);
            }
            if (leftStickVertAxis < 0) {
                return updateFocus(game.getStage(), true);
            }
        }
        return false;
    }

}
