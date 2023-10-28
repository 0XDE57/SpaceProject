package com.spaceproject.ui.menu;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CargoComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;


public class SpaceStationMenu {

    public static Actor SpaceStationMenu(ControllerMenuStage stage, Entity player, Engine engine) {
        //enable color markup
        VisUI.getSkin().get("default-font", BitmapFont.class).getData().markupEnabled = true;

        boolean controllerHasFocus = engine.getSystem(DesktopInputSystem.class).getControllerHasFocus();
        KeyConfig keyConfig = SpaceProject.configManager.getConfig(KeyConfig.class);
        String hyperControl = controllerHasFocus ? "B" : Input.Keys.toString(keyConfig.activateHyperDrive);
        String shieldControl = controllerHasFocus ? "LT" : Input.Keys.toString(keyConfig.activateShield);
        String shieldUpgrade = "Active Shield".toUpperCase();
        String hyperdriveUpgrade = "Hyper-Drive".toUpperCase();
        String colorItem = "#14B1FFFF";
        String colorCredits = "#77FF14";
        String colorControl = "#cb6701";
        String hyperdriveDescription = "[" + colorItem + "]" + hyperdriveUpgrade + "[]\nenable deep-space exploration.\n\nHold [" + colorControl + "][" + hyperControl + "][] to activate.";
        String shieldDescription = "[" + colorItem + "]" + shieldUpgrade + "[]\nprotect your ship from damage.\n\nHold [" + colorControl + "][" + shieldControl + "][] to activate.";
        int costHyper = 999999;
        int costShield = 10000;

        ScrollableTextArea text = new ScrollableTextArea("");
        Table creditsTable = new VisTable();
        Label creditsText = new VisLabel("CREDITS: ");
        creditsTable.add(creditsText);
        Label creditsValue = new VisLabel("[" + colorCredits + "]" + Mappers.cargo.get(player).credits);
        //creditsValue.addAction(forever(sequence(color(Color.RED,0.2f), color(Color.BLUE, 0.2f), delay(0.5f))));
        creditsTable.add(creditsText);
        creditsTable.add(creditsValue);

        TextButton buttonHyperDrive = new TextButton("[" + colorItem + "]" + hyperdriveUpgrade + "  [" + colorCredits + "]" + costHyper, VisUI.getSkin());
        buttonHyperDrive.getLabel().setAlignment(Align.left);
        buttonHyperDrive.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Mappers.hyper.get(player) != null) return;

                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costHyper) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for " + hyperdriveUpgrade);
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //purchase
                cargo.credits -= costHyper;
                creditsValue.setText(Mappers.cargo.get(player).credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));

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
        buttonHyperDrive.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText(hyperdriveDescription);
                return super.handle(event);
            }
        });

        TextButton buttonShield = new TextButton("[" + colorItem + "]" + shieldUpgrade + "  [" + colorCredits + "]" + costShield, VisUI.getSkin());
        buttonShield.getLabel().setAlignment(Align.left);
        buttonShield.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Mappers.shield.get(player) != null) return;

                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costShield) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for " + shieldUpgrade);
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //purchase
                cargo.credits -= costShield;
                creditsValue.setText(Mappers.cargo.get(player).credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));

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
        buttonShield.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText(shieldDescription);
                return super.handle(event);
            }
        });

        Table buttonTable = new VisTable();
        buttonTable.add(buttonShield).fillX().row();
        buttonTable.add(buttonHyperDrive).fillX().row();

        Table descTable = new VisTable();
        descTable.add(text.createCompatibleScrollPane()).growX().height(150).row();

        VisWindow window = new VisWindow("[#0090d2]STATION DELTA");
        window.getTitleLabel().setAlignment(Align.center);

        window.add(buttonTable).pad(10);
        window.add(descTable).pad(10).row();
        window.add(creditsTable).expandX().fillX();

        for (Actor button : buttonTable.getChildren()) {
            stage.addFocusableActor(button);
        }

        window.pack();
        stage.addActor(window);
        window.setPosition(Gdx.graphics.getWidth() / 3f, Gdx.graphics.getHeight() / 2f, Align.center);
        stage.setFocusedActor(buttonTable.getChildren().first());

        return window;
    }
}
