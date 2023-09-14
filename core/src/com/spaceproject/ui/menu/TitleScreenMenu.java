package com.spaceproject.ui.menu;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.widget.ButtonBar;
import com.kotcrab.vis.ui.widget.LinkLabel;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.spaceproject.SpaceProject;
import com.spaceproject.screens.debug.BlocksTestScreen;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.debug.Test3DScreen;
import com.spaceproject.screens.debug.TestNoiseScreen;
import com.spaceproject.screens.debug.TestShipGenerationScreen;
import com.spaceproject.screens.debug.TestSpiralGalaxy;
import com.spaceproject.screens.debug.TestVoronoiScreen;
import com.spaceproject.utility.IndependentTimer;

public class TitleScreenMenu implements ControllerListener {

    private final Stage stage;
    public Table table;
    private final Array<TextButton> buttons;
    private int focusIndex = -1;
    private final IndependentTimer lastFocusTimer;
    private float leftStickVertAxis;

    public TitleScreenMenu(final Stage stage, final SpaceProject game, boolean showDebugScreens) {
        this.stage = stage;
        table = new Table();
        buttons = new Array<>();
        lastFocusTimer = new IndependentTimer(200, true);

        if (showDebugScreens) {
            addDebugItems(game);
        }
    
        addMenuItems(game);
    
        //set bigger labels on mobile
        if (SpaceProject.isMobile()) {
            for (Actor button : table.getChildren()) {
                if (button instanceof TextButton) {
                    ((TextButton) button).getLabel().setFontScale(2f);
                }
            }
        }

        setFocusItems();

        stage.addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                removeFocus();
                return super.mouseMoved(event, x, y);
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                switch (keycode) {
                    case Input.Keys.W:
                    case Input.Keys.UP:
                        return updateFocus(true);
                    case Input.Keys.S:
                    case Input.Keys.DOWN:
                        return updateFocus(false);
                    case Input.Keys.SPACE:
                    case Input.Keys.ENTER:
                        return selectFocusedActor();
                }
                return super.keyDown(event, keycode);
            }
        });
    }

    private void setFocusItems() {
        buttons.clear();
        for (Actor element : table.getChildren()) {
            buttons.add((TextButton) element);
        }
    }

    private boolean updateFocus(boolean up) {
        if (buttons.size == 0) return false;
        if (up) {
            focusIndex--;
        } else {
            focusIndex++;
        }
        focusIndex = focusIndex % buttons.size;
        if (focusIndex < 0) {
            focusIndex = buttons.size - 1;
        }
        //skip disabled elements, move to next item
        if (buttons.get(focusIndex).isDisabled()) {
            return updateFocus(up); //warning: StackOverflow if ALL buttons are disabled (which shouldn't happen)
        }
        stage.setKeyboardFocus(buttons.get(focusIndex));
        return true;
    }

    private void removeFocus() {
        focusIndex = -1;
        stage.setKeyboardFocus(null);
    }

    private boolean selectFocusedActor() {
        if (focusIndex == -1) return false;

        InputEvent touchEvent = new InputEvent();
        touchEvent.setType(InputEvent.Type.touchDown);
        buttons.get(focusIndex).fire(touchEvent);

        touchEvent = new InputEvent();
        touchEvent.setType(InputEvent.Type.touchUp);
        buttons.get(focusIndex).fire(touchEvent);
        return true;
    }

    private void addMenuItems(final SpaceProject game) {
        TextButton btnPlay = new TextButton("play", VisUI.getSkin());
        btnPlay.getLabel().setAlignment(Align.left);
        btnPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen());
            }
        });

        TextButton btnLoad = new TextButton("load", VisUI.getSkin());
        btnLoad.getLabel().setAlignment(Align.left);
        btnLoad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(stage, "load", "not implemented yet");
            }
        });

        TextButton btnOption = new TextButton("options", VisUI.getSkin());
        btnOption.getLabel().setAlignment(Align.left);
        btnOption.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // todo: open settings panel with key mapping and such
                // todo: make settings panel
                Dialogs.showOKDialog(stage, "options", "not implemented yet");
            }
        });

        TextButton btnAbout = new TextButton("about", VisUI.getSkin());
        btnAbout.getLabel().setAlignment(Align.left);
        btnAbout.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showAboutDialog(stage);
            }
        });

        TextButton btnExit = new TextButton("exit", VisUI.getSkin());
        btnExit.getLabel().setAlignment(Align.left);
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOptionDialog(stage, "exit", "goodbye?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        Gdx.app.exit();
                    }
                });
            }
        });

        table.add(btnPlay).fillX().row();
        table.add(btnLoad).fillX().row();
        table.add(btnOption).fillX().row();
        table.add(btnAbout).fillX().row();
        table.add(btnExit).fillX().row();
    }

    public void addDebugItems(final SpaceProject game) {
        TextButton btnVoronoi = new TextButton("[DEBUG] voronoi", VisUI.getSkin());
        btnVoronoi.getLabel().setAlignment(Align.left);
        btnVoronoi.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestVoronoiScreen());
            }
        });

        TextButton btnNoise = new TextButton("[DEBUG] noise", VisUI.getSkin());
        btnNoise.getLabel().setAlignment(Align.left);
        btnNoise.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestNoiseScreen());
            }
        });

        TextButton btnShip = new TextButton("[DEBUG] ship gen", VisUI.getSkin());
        btnShip.getLabel().setAlignment(Align.left);
        btnShip.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestShipGenerationScreen());
            }
        });

        TextButton btn3D = new TextButton("[DEBUG] 3D rotate", VisUI.getSkin());
        btn3D.getLabel().setAlignment(Align.left);
        btn3D.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new Test3DScreen());
            }
        });
        
        TextButton btnSpiral = new TextButton("[DEBUG] Spiral Gen", VisUI.getSkin());
        btnSpiral.getLabel().setAlignment(Align.left);
        btnSpiral.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestSpiralGalaxy());
            }
        });
    
        TextButton btnBlock = new TextButton("[DEBUG] Block Engine Test", VisUI.getSkin());
        btnBlock.getLabel().setAlignment(Align.left);
        btnBlock.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new BlocksTestScreen());
            }
        });

        table.add(btnBlock).fillX().row();
        table.add(btnSpiral).fillX().row();
        table.add(btnVoronoi).fillX().row();
        table.add(btnNoise).fillX().row();
        table.add(btn3D).fillX().row();
        table.add(btnShip).fillX().row();

        removeFocus();
        setFocusItems();
    }
    
    private VisDialog showAboutDialog(Stage stage) {
        final VisDialog dialog = new VisDialog("");
        dialog.closeOnEscape();
        dialog.centerWindow();

        String aboutText = "Shoot some asteroids, they fall apart!";
        aboutText += "\nversion: " + SpaceProject.VERSION
                + "\nlibGDX: " +  com.badlogic.gdx.Version.VERSION;
        aboutText += "\nDeveloped with <3";
        dialog.text(aboutText);
        
        LinkLabel link = new LinkLabel("https://github.com/0xDE57/SpaceProject");
        link.setListener(url -> Gdx.net.openURI(url));
        dialog.getContentTable().row();
        dialog.getContentTable().add(link);

        dialog.button(ButtonBar.ButtonType.OK.getText());
        dialog.pad(30.0f).padBottom(10.0f);
        
        dialog.addListener(new InputListener() {
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    dialog.fadeOut();
                    return true;
                } else {
                    return false;
                }
            }
        });

        dialog.pack();
        stage.addActor(dialog.fadeIn());
        return dialog;
    }

    @Override
    public void connected(Controller controller) {}

    @Override
    public void disconnected(Controller controller) {}

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        if (buttonCode == controller.getMapping().buttonDpadUp) {
            return updateFocus(true);
        }
        if (buttonCode == controller.getMapping().buttonDpadDown) {
            return updateFocus(false);
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
        if (axisCode == controller.getMapping().axisLeftY) {
            leftStickVertAxis = value;
        }
        if (Math.abs(leftStickVertAxis) > 0.6f && lastFocusTimer.tryEvent()) {
            if (leftStickVertAxis > 0) {
                return updateFocus(false);
            }
            if (leftStickVertAxis < 0) {
                return updateFocus(true);
            }
        }
        return false;
    }

}
