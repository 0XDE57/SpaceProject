package com.spaceproject.ui.menu.tabs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.ui.menu.GameMenu;

import static com.spaceproject.screens.MyScreenAdapter.game;

public class MainMenuTab extends HotKeyTab {
    
    private int itemPadding = 2;
    
    public MainMenuTab(final GameMenu gameMenu) {
        super("GameMenu", Input.Keys.ESCAPE);
        
        
        TextButton btnGotoMain = new TextButton("main menu", VisUI.getSkin());
        btnGotoMain.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOptionDialog(gameMenu.getStage(), "Exit", "go to the main menu?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        game.setScreen(new TitleScreen(game));
                    }
                });
            }
        });
    
    
        TextButton btnLoad = new TextButton("load", VisUI.getSkin());
        btnLoad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(gameMenu.getStage(), "load", "not implemented yet");
            }
        });
        
    
        TextButton btnSave = new TextButton("save", VisUI.getSkin());
        btnSave.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(gameMenu.getStage(), "save", "not implemented yet");
            }
        });
        
        
        TextButton btnOptions = new TextButton("options", VisUI.getSkin());
        btnOptions.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                TabbedPane tabbedPane = gameMenu.getTabbedPane();
                Tab keyConfigTab = gameMenu.getKeyConfigTab();
                if (!tabbedPane.getTabs().contains(keyConfigTab, false)) {
                    tabbedPane.add(keyConfigTab);
                }
                tabbedPane.switchTab(keyConfigTab);
            }
        });
        
    
        TextButton btnExit = new TextButton("exit", VisUI.getSkin());
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOptionDialog(gameMenu.getStage(), "Exit", "close game?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        Gdx.app.exit();
                    }
                });
            }
        });
    
        
        getContentTable().add(btnGotoMain).growX().pad(itemPadding).row();
        getContentTable().add(btnSave).growX().pad(itemPadding).row();
        getContentTable().add(btnLoad).growX().pad(itemPadding).row();
        getContentTable().add(btnOptions).growX().pad(itemPadding).row();
        getContentTable().add(btnExit).growX().pad(itemPadding).row();
    }
    
}
