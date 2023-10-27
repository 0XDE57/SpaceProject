package com.spaceproject.ui.menu;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisTable;
import com.spaceproject.components.CargoComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class SpaceStationMenu {

    public static Table SpaceStationMenu(ControllerMenuStage stage, Entity player) {
        int costHyper = 999999;
        TextButton buttonHyperDrive = new TextButton("HYPER-DRIVE: " + costHyper, VisUI.getSkin());
        buttonHyperDrive.getLabel().setAlignment(Align.left);
        buttonHyperDrive.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Mappers.hyper.get(player) != null) return;

                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costHyper) {
                    //error soundfx if not enough moneys
                    //setHint("insufficient credits");
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for HYPERDRIVE");
                    return;
                }
                //purchase
                cargo.credits -= costHyper;
                //add new hyperdrive
                HyperDriveComponent hyperDrive = new HyperDriveComponent();
                hyperDrive.speed = 2000;
                hyperDrive.coolDownTimer = new SimpleTimer(2000);
                hyperDrive.chargeTimer = new SimpleTimer(2000);
                hyperDrive.graceTimer = new SimpleTimer(1000);
                player.add(hyperDrive);
                //getEngine().getSystem(SpaceStationSystem.class).purchase(player,HyperDrive.class);
            }
        });

        int costShield = 10000;
        TextButton buttonShield = new TextButton("Active Shield: " + costShield, VisUI.getSkin());
        buttonShield.getLabel().setAlignment(Align.left);
        buttonShield.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Mappers.shield.get(player) != null) return;

                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costShield) {
                    //error soundfx if not enough moneys
                    //setHint("insufficient credits");
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for Shield");
                    return;
                }
                //purchase
                cargo.credits -= costShield;
                //add new shield
                ShieldComponent shield = new ShieldComponent();
                shield.animTimer = new SimpleTimer(100, true);
                shield.defence = 100f;
                Rectangle dimensions = Mappers.vehicle.get(player).dimensions;
                float radius = Math.max(dimensions.getWidth(), dimensions.getHeight());
                shield.maxRadius = radius;
                shield.lastHit = GameScreen.getGameTimeCurrent() - 1000;
                shield.heatResistance = 0f;
                shield.cooldownRate = 0.1f;
                player.add(shield);
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
