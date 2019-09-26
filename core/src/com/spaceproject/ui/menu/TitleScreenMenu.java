package com.spaceproject.ui.menu;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.spaceproject.SpaceProject;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.debug.Test3DScreen;
import com.spaceproject.screens.debug.TestNoiseScreen;
import com.spaceproject.screens.debug.TestShipGenerationScreen;
import com.spaceproject.screens.debug.TestVoronoiScreen;

public class TitleScreenMenu {
    
    public static Table buildMenu(final SpaceProject game, final Stage stage, boolean showDebugScreens) {
        
        TextButton btnPlay = new TextButton("play", VisUI.getSkin());
        btnPlay.getLabel().setAlignment(Align.left);
        btnPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(true));
            }
        });
        
        
        TextButton btnVoronoi = new TextButton("voronoi [DEBUG]", VisUI.getSkin());
        btnVoronoi.getLabel().setAlignment(Align.left);
        btnVoronoi.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestVoronoiScreen());
            }
        });
        
        
        TextButton btnNoise = new TextButton("noise [DEBUG]", VisUI.getSkin());
        btnNoise.getLabel().setAlignment(Align.left);
        btnNoise.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestNoiseScreen());
            }
        });
        
        
        TextButton btnShip = new TextButton("ship gen [DEBUG]", VisUI.getSkin());
        btnShip.getLabel().setAlignment(Align.left);
        btnShip.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestShipGenerationScreen());
            }
        });
        
        
        TextButton btn3D = new TextButton("3D rotate [DEBUG]", VisUI.getSkin());
        btn3D.getLabel().setAlignment(Align.left);
        btn3D.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new Test3DScreen());
            }
        });
        
        /*
        TextButton btnSpiral = new TextButton("Spiral Gen [DEBUG]", VisUI.getSkin());
        btnSpiral.getLabel().setAlignment(Align.left);
        btnSpiral.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestSpiralGalaxy());
            }
        });*/
        
        
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
                Dialogs.showOKDialog(stage, "options", "not implemented yet");
            }
        });
        
        //about
        TextButton btnAbout = new TextButton("about", VisUI.getSkin());
        btnAbout.getLabel().setAlignment(Align.left);
        btnAbout.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(stage, "about", "a space game (WIP...)\nDeveloped by Whilow Schock");
            }
        });
        
        TextButton btnExit = new TextButton("exit", VisUI.getSkin());
        btnExit.getLabel().setAlignment(Align.left);
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        
        
        //add buttons to table
        Table table = new Table();
        table.add(btnPlay).fillX().row();
        if (showDebugScreens) {
            table.add(btnVoronoi).fillX().row();
            table.add(btnNoise).fillX().row();
            table.add(btn3D).fillX().row();
            table.add(btnShip).fillX().row();
        }
        table.add(btnLoad).fillX().row();
        table.add(btnOption).fillX().row();
        table.add(btnAbout).fillX().row();
        table.add(btnExit).fillX().row();
        
        
        //set bigger labels on mobile
        if (SpaceProject.isMobile()) {
            for (Actor button : table.getChildren()) {
                if (button instanceof TextButton) {
                    //((TextButton) button).getLabel().getFont???.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
                    ((TextButton) button).getLabel().setFontScale(2f);
                }
            }
        }
        
        
        return table;
    }
    
}
