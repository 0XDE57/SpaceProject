package com.spaceproject.ui.menu;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.spaceproject.SpaceProject;
import com.spaceproject.screens.debug.BlocksTestScreen;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.debug.*;
import com.spaceproject.ui.ControllerMenuStage;

public class TitleScreenMenu {

    private final ControllerMenuStage stage;
    public Table table;

    public TitleScreenMenu(final ControllerMenuStage stage, final SpaceProject game, boolean showDebugScreens) {
        this.stage = stage;
        table = new Table();

        if (showDebugScreens) {
            addDebugItems(game);
        }
    
        addMenuItems(game);
        for (Actor button : table.getChildren()) {
            stage.addFocusableActor(button);
        }

        //set bigger labels on mobile
        if (SpaceProject.isMobile()) {
            for (Actor button : table.getChildren()) {
                if (button instanceof TextButton) {
                    ((TextButton) button).getLabel().setFontScale(2f);
                }
            }
        }
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
                VisDialog dialog = MyDialogs.showOKDialog(stage, "load", "not implemented yet");
                dialog.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        System.out.println("changed?");
                    }
                });
            }
        });

        TextButton btnOption = new TextButton("options", VisUI.getSkin());
        btnOption.getLabel().setAlignment(Align.left);
        btnOption.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // todo: open settings panel with key mapping and such
                // todo: make settings panel
                MyDialogs.showMultiDialog(stage, "[multibuttondialog]",  "[WASD] or arrow keys or [D-Pad] or Left-Joystick to navigate");
            }
        });

        TextButton btnAbout = new TextButton("about", VisUI.getSkin());
        btnAbout.getLabel().setAlignment(Align.left);
        btnAbout.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showAboutDialog(stage);
            }
        });

        TextButton btnExit = new TextButton("exit", VisUI.getSkin());
        btnExit.getLabel().setAlignment(Align.left);

        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showYesNoDialog(stage, "exit", "goodbye?", new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        Gdx.app.exit();
                    }
                });
            }
        });
        stage.setEscapeActor(btnExit);

/*
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                final int nothing = 1;
                final int everything = 2;
                final int something = 3;

                //confirmdialog may return result of any type, here we are just using ints
                Dialogs.showConfirmDialog(stage, "confirm dialog", "what do you want?",
                        new String[]{"nothing", "everything", "something"}, new Integer[]{nothing, everything, something},
                        result -> {
                            switch (result) {
                                case nothing: Dialogs.showOKDialog(stage, "result", "pressed: nothing"); break;
                                case everything: Dialogs.showOKDialog(stage, "result", "pressed: everything"); break;
                                case something: Dialogs.showOKDialog(stage, "result", "pressed: something"); break;
                            }
                        });

                MyDialogs.showCustomDialog(stage, "confirm dialog", "what do you want?",
                        new String[]{"nothing", "everything", "something"}, new Integer[]{nothing, everything, something},
                        result -> {
                            switch (result) {
                                case nothing: Dialogs.showOKDialog(stage, "result", "pressed: nothing"); break;
                                case everything: Dialogs.showOKDialog(stage, "result", "pressed: everything"); break;
                                case something: Dialogs.showOKDialog(stage, "result", "pressed: something"); break;
                            }
                        });

                }
        });*/

        btnLoad.setDisabled(true);
        //btnOption.setDisabled(true);
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
    }

}
