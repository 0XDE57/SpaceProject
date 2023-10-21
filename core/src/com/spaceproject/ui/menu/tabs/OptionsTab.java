package com.spaceproject.ui.menu.tabs;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.ConfigManager;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;

//Controller tab?
public class OptionsTab extends Tab {
    private String title;
    private Table content;

    public OptionsTab(String title, GameScreen game) {
        super(false, false);
        this.title = title;
        content = new VisTable();
        content.setFillParent(true);
        EngineConfig engineConfig = SpaceProject.configManager.getConfig(EngineConfig.class);
        KeyConfig keyConfig = SpaceProject.configManager.getConfig(KeyConfig.class);

        final VisCheckBox toggleFullscreen = new VisCheckBox("fullscreen [" + Input.Keys.toString(keyConfig.fullscreen) + "]", Gdx.graphics.isFullscreen());
        toggleFullscreen.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (toggleFullscreen.isChecked()) {
                    game.setFullscreen();
                } else {
                    game.setWindowedMode();
                }
            }
        });

        final VisCheckBox toggleVsync = new VisCheckBox("vsync ["+ Input.Keys.toString(keyConfig.vsync) + "]", engineConfig.vsync);
        toggleVsync.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setVsync(toggleVsync.isChecked());
            }
        });

        content.add(toggleFullscreen).left().row();
        content.add(toggleVsync).left().row();
        content.pack();
    }


    @Override
    public String getTabTitle() {
        return title;
    }

    @Override
    public Table getContentTable() {
        return content;
    }

}
