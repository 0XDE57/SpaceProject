package com.spaceproject.ui.menu.tabs;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;

public class OptionsTab extends Tab {
    private String title;
    private Table content;

    public OptionsTab(String title) {
        super(false, false);
        this.title = title;
        content = new VisTable();
        content.setFillParent(true);
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
