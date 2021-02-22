package com.spaceproject.ui.debug.nodes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.ui.debug.ECSExplorerWindow;
import com.spaceproject.utility.Misc;
import com.spaceproject.utility.SimpleTimer;

import java.lang.reflect.Field;

import static com.spaceproject.generation.FontFactory.skinSmallFont;

/**
 * Created by Whilow Schock on 25/09/2019.
 */
public class ReflectionNode extends UpdateNode {
    
    boolean isNew = false;
    private SimpleTimer newTimer;
    
    public ReflectionNode(Object object) {
        super(new Label(Misc.objString(object), VisUI.getSkin(), skinSmallFont, Color.WHITE), object);
        init();
    }
    
    public ReflectionNode(Object object, boolean markNew) {
        this(object);
        
        isNew = markNew;
        if (isNew) {
            newTimer = new SimpleTimer(ECSExplorerWindow.newTime, true);
            getActor().setColor(Color.GREEN);
        }
    }
    
    private void init() {
        for (Field f : getValue().getClass().getFields()) {
            add(new FieldNode(new Label("init", VisUI.getSkin(), skinSmallFont, Color.WHITE), getValue(), f));
        }
    }
    
    @Override
    public void update() {
        if (isNew && newTimer.tryEvent()) {
            isNew = false;
            newTimer = null;
            getActor().setColor(Color.WHITE);
        }
        
        if (!isExpanded())
            return;
        
        for (Object node : getChildren())
            ((FieldNode) node).update();
        
    }
    
    @Override
    public String toString() {
        if (getValue() == null)
            return super.toString();
        
        return Misc.objString(getValue());
    }
}
