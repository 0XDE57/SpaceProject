package com.spaceproject.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.spaceproject.ui.ControllerMenuStage;

public class SpaceStationMenu {

    public static Table SpaceStationMenu(ControllerMenuStage stage) {
        TextButton buttonHyperDrive = new VisTextButton("HYPER-DRIVE");
        buttonHyperDrive.getLabel().setAlignment(Align.left);
        buttonHyperDrive.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.debug(getClass().getSimpleName(), "do something");
            }
        });

        TextButton buttonShield = new VisTextButton("Active Shield");
        buttonShield.getLabel().setAlignment(Align.left);
        buttonShield.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.debug(getClass().getSimpleName(), "do something");
            }
        });

        Table table = new VisTable();
        table.add(buttonHyperDrive).fillX().row();
        table.add(buttonShield).fillX().row();
        table.pack();

        for (Actor button : table.getChildren()) {
            stage.addFocusableActor(button);
        }

        stage.addActor(table);
        table.setPosition(Gdx.graphics.getWidth() / 3f, Gdx.graphics.getHeight() / 2f, Align.center);
        stage.setFocusedActor(table.getChildren().first());
        return table;
    }
}
