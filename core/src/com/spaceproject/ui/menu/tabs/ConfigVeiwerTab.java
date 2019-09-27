package com.spaceproject.ui.menu.tabs;


import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.config.Config;
import com.spaceproject.ui.menu.GameMenu;

import java.util.ArrayList;

public class ConfigVeiwerTab extends HotKeyTab {
    
    private int itemPadding = 2;
    
    public ConfigVeiwerTab(final GameMenu gameMenu, ArrayList<Config> configurations) {
        super("Config List", Input.Keys.UNKNOWN);
        
        
        for (Config config : configurations) {
            TextButton editButton = new TextButton(config.getClass().getSimpleName(), VisUI.getSkin());
            editButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    //gameMenu.getStage()
                    
                }
            });
    
            getContentTable().add(editButton).growX().pad(itemPadding).row();
        }
    }
    
}
