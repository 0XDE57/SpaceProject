package com.spaceproject.ui.menu.tabs;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.spaceproject.config.Config;

public abstract class ConfigTab extends Tab {
    private String title;
    private Table content;
    VisTable scrollContainer;
    
    Config localConfig;
    int padSize = 2;
    
    public ConfigTab(String title, Config config) {
        super(true, true);
        this.title = title;
        this.localConfig = config;
        
        content = new VisTable();
        content.setFillParent(true);
        
        scrollContainer = new VisTable();
        scrollContainer.left().top();
        buildTab(localConfig);
        final VisScrollPane scrollPane = new VisScrollPane(scrollContainer);
        scrollPane.addListener(autoFocusScroll(scrollPane));
        scrollPane.setFlickScroll(false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(false, false);
        
        getContentTable().add(scrollPane).left().top().expand().fill();
        
        getContentTable().row();
        //getContentTable().add(new Separator()).fillX().row();
        //getContentTable().add(new TextButton("save", VisUI.getSkin()));
        //getContentTable().add(new TextButton("reset", VisUI.getSkin()));
    }
    
    private EventListener autoFocusScroll(final VisScrollPane scrollPane) {
        return new EventListener() {
            //auto focus scroll on mouse enter
            @Override
            public boolean handle(Event event) {
                if (event instanceof InputEvent)
                    if (((InputEvent) event).getType() == InputEvent.Type.enter)
                        event.getStage().setScrollFocus(scrollPane);
                return false;
            }
        };
    }
    
    public abstract void buildTab(Object config);
    
    @Override
    public void onHide() {
        Gdx.app.log(this.getClass().getSimpleName(), "configtab hide");
        if (isDirty()) {
            TabbedPane pane = getPane();
            if (pane != null) {
                pane.switchTab(this);//keep focus on this
                pane.remove(this, false);//trigger save changes dialog
            } else {
                discardChanges();
            }
        } else {
            super.onHide();
        }
    }
    
    @Override
    public String getTabTitle() {
        return title;
    }
    
    @Override
    public Table getContentTable() {
        return content;
    }
    
    @Override
    public boolean save() {
        saveChanges();
        return super.save();
    }
    
    public abstract void saveChanges();
    
    public abstract void discardChanges();
    
}