package com.spaceproject.ui.menu.debug.nodes;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import java.lang.reflect.Field;

/**
 * Created by Whilow Schock on 25/09/2019.
 */
public class FieldNode extends UpdateNode {
    
    private Object owner;
    
    public FieldNode(Actor actor, Object owner, Field field) {
        super(actor, field);
        this.owner = owner;
        getActor().setName(toString());
        update();
    }
    
    private Object getOwner() {
        return owner;
    }
    
    @Override
    public void update() {
        ((Label) getActor()).setText(toString());
    }
    
    @Override
    public String toString() {
        if (getValue() == null || getOwner() == null) {
            return super.toString();
        }
        try {
            return String.format("%-14s %s", ((Field) getValue()).getName(), ((Field) getValue()).get(getOwner()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
