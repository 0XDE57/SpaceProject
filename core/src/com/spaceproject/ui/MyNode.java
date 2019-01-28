package com.spaceproject.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;


public class MyNode extends Tree.Node {

    private long id;
    public MyNode(Actor actor, long id) {
        super(actor);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof MyNode) {
                return ((MyNode) o).id == this.id;
            }
        }
        return super.equals(o);
    }
}
