package com.spaceproject.ui;


import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;

public class HotKeyTab extends Tab {
    private String title;
    private Table content;
    private int hotKey;

    HotKeyTab(String title, int hotKey) {
        super(false, false);
        this.hotKey = hotKey;
        this.title = title + " [" + Input.Keys.toString(hotKey) + "]";

        content = new VisTable();
        content.setFillParent(true);

    }

    int getHotKey() {
        return hotKey;
    }

    @Override
    public String getTabTitle () {
        return title;
    }

    @Override
    public Table getContentTable () {
        return content;
    }

}