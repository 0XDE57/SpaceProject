package com.spaceproject.ui.menu;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.spaceproject.SpaceProject;
import com.spaceproject.screens.debug.BlocksTestScreen;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.debug.Test3DScreen;
import com.spaceproject.screens.debug.TestNoiseScreen;
import com.spaceproject.screens.debug.TestShipGenerationScreen;
import com.spaceproject.screens.debug.TestSpiralGalaxy;
import com.spaceproject.screens.debug.TestVoronoiScreen;

public class TitleScreenMenu {

    private final Stage stage;
    public Table table;
    private final Array<TextButton> buttons;
    private int focusIndex = -1;

    public TitleScreenMenu(final Stage stage, final SpaceProject game, boolean showDebugScreens) {
        this.stage = stage;
        table = new Table();
        buttons = new Array<>();

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
                        focusIndex--;
                        updateFocus();
                        break;
                    case Input.Keys.S:
                    case Input.Keys.DOWN:
                        focusIndex++;
                        updateFocus();
                        break;
                    case Input.Keys.SPACE:
                    case Input.Keys.ENTER:
                        if (focusIndex >= 0 && focusIndex < buttons.size) {
                            InputEvent touchEvent = new InputEvent();
                            touchEvent.setType(InputEvent.Type.touchDown);
                            buttons.get(focusIndex).fire(touchEvent);

                            touchEvent = new InputEvent();
                            touchEvent.setType(InputEvent.Type.touchUp);
                            buttons.get(focusIndex).fire(touchEvent);
                        }
                        break;
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

    private void updateFocus() {
        if (buttons.size == 0) return;
        focusIndex = focusIndex % buttons.size;
        if (focusIndex < 0) {
            focusIndex = buttons.size - 1;
        }
        stage.setKeyboardFocus(buttons.get(focusIndex));
    }

    private void removeFocus() {
        focusIndex = -1;
        stage.setKeyboardFocus(null);
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

}
