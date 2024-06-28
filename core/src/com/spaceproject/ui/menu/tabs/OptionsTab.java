package com.spaceproject.ui.menu.tabs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.systems.HUDSystem;
import com.spaceproject.systems.SoundSystem;

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

        final VisLabel volumeText = new VisLabel("volume: 1");
        final VisSlider volumeSlider = new VisSlider(0, 1, 0.1f, false);
        volumeSlider.setValue(volumeSlider.getMaxValue());
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float volume = volumeSlider.getValue();
                GameScreen.getEngine().getSystem(SoundSystem.class).setVolume(volume);
                volumeText.setText("volume: " + (MathUtils.isEqual(volume, 0) ? "OFF" : MyMath.round(volume,1)));
            }
        });


        final VisCheckBox toggleDamageNumbers = new VisCheckBox("show damage numbers (experimental)", HUDSystem.showDamageNumbers);
        toggleDamageNumbers.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                HUDSystem.showDamageNumbers = toggleDamageNumbers.isChecked();
            }
        });

        content.add(toggleFullscreen).left().row();
        content.add(toggleVsync).left().row();
        int pad = 10;
        //getContentTable().add(new Separator()).fillX().padTop(pad).padBottom(pad).row();
        content.add(volumeText).padTop(6).row();
        content.add(volumeSlider).fillX().padBottom(6).row();
        //getContentTable().add(new Separator()).fillX().padTop(pad).padBottom(pad).row();
        content.add(toggleDamageNumbers).left().row();
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
