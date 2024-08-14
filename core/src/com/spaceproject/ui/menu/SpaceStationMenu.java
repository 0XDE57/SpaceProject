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
import com.spaceproject.components.*;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;


public class SpaceStationMenu {

    //todo: move item cost into own actor to fix alignment issues
    //todo: gray out upgrades that are already obtained
    public static Actor SpaceStationMenu(ControllerMenuStage stage, Entity player, Engine engine) {
        //enable color markup
        VisUI.getSkin().get("default-font", BitmapFont.class).getData().markupEnabled = true;
        String titleColor = "#0090d2";
        String colorItem = "#14B1FFFF";
        String colorCredits = "#77FF14";
        String colorControl = "#cb6701";

        boolean controllerHasFocus = engine.getSystem(DesktopInputSystem.class).getControllerHasFocus();
        KeyConfig keyConfig = SpaceProject.configManager.getConfig(KeyConfig.class);
        String hyperControl = controllerHasFocus ? "B" : Input.Keys.toString(keyConfig.activateHyperDrive);
        String shieldControl = controllerHasFocus ? "LT" : Input.Keys.toString(keyConfig.activateShield);
        String laserControl = controllerHasFocus ? "RT" : "L-Click";
        String shieldUpgrade = "Active Shield".toUpperCase();
        String hyperdriveUpgrade = "Hyper-Drive".toUpperCase();
        String laserUpgrade = "Laser".toUpperCase();
        String tractorUpgrade = "Tractor Beam".toUpperCase();
        String hyperdriveDescription = "[" + colorItem + "]" + hyperdriveUpgrade + "[]\nenable deep-space exploration.\n\nHold [" + colorControl + "][" + hyperControl + "][] to activate.";
        String shieldDescription = "[" + colorItem + "]" + shieldUpgrade + "[]\nprotect your ship from damage.\n\nHold [" + colorControl + "][" + shieldControl + "][] to activate.";
        String laserDescription = "[" + colorItem + "]" + laserUpgrade + "[]\nprecision tool.\n\nHold [" + colorControl + "][" + laserControl + "][] to activate.";
        String tractorBeamDescription = "[" + colorItem + "]" + tractorUpgrade + "[]\npush or pull objects.\n\nHold [" + colorControl + "][" + laserControl + "][] to activate.";
        int costHyper = 50000;
        int costShield = 5000;
        int costLaser = 20000;
        int costTractorBeam = 10000;
        int costHP = 10000;
        int costThrust = 1000;
        int costDMG = 1000;

        ScrollableTextArea text = new ScrollableTextArea("");
        text.removeListener(text.getDefaultInputListener());
        Table creditsTable = new VisTable();
        Label creditsText = new VisLabel("CREDITS: ");
        creditsTable.add(creditsText);
        Label creditsValue = new VisLabel("[" + colorCredits + "]" + Mappers.cargo.get(player).credits);
        //creditsValue.addAction(forever(sequence(color(Color.RED,0.2f), color(Color.BLUE, 0.2f), delay(0.5f))));
        creditsTable.add(creditsText);
        creditsTable.add(creditsValue);

        TextButton buttonHyperDrive = new TextButton("[" + colorItem + "]" + hyperdriveUpgrade + "   [" + colorCredits + "]" + costHyper, VisUI.getSkin());
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
        VehicleComponent vehicle = Mappers.vehicle.get(player);
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
                shield.animTimer = new SimpleTimer(50, true);
                shield.defence = 100f;
                Rectangle dimensions = vehicle.dimensions;
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

        TextButton buttonLaser = new TextButton("[" + colorItem + "]" + laserUpgrade + "  [" + colorCredits + "]               " + costLaser, VisUI.getSkin());
        buttonLaser.getLabel().setAlignment(Align.left);
        buttonLaser.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Mappers.laser.get(player) != null) return;
                if (vehicle.tools.containsKey(VehicleComponent.Tool.laser.ordinal())) return;

                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costLaser) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for " + laserUpgrade);
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //purchase
                cargo.credits -= costLaser;
                creditsValue.setText(Mappers.cargo.get(player).credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));

                int referenceWavelength = 589;//"yellow doublet" sodium D line
                LaserComponent laser = new LaserComponent(520, 250, 30, 1);
                vehicle.tools.put(VehicleComponent.Tool.laser.ordinal(), laser);
            }
        });
        buttonLaser.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText(laserDescription);
                return super.handle(event);
            }
        });

        TextButton buttonTractorBeam = new TextButton("[" + colorItem + "]" + tractorUpgrade + "  [" + colorCredits + "] " + costTractorBeam, VisUI.getSkin());
        buttonTractorBeam.getLabel().setAlignment(Align.left);
        buttonTractorBeam.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Mappers.tractor.get(player) != null) return;
                if (vehicle.tools.containsKey(VehicleComponent.Tool.tractor.ordinal())) return;

                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costTractorBeam) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for " + tractorUpgrade);
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //purchase
                cargo.credits -= costTractorBeam;
                creditsValue.setText(Mappers.cargo.get(player).credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));

                //add upgrade to tools inventory
                TractorBeamComponent tractorBeam = new TractorBeamComponent();
                tractorBeam.maxDist = 150;
                tractorBeam.magnitude = 70000;
                vehicle.tools.put(VehicleComponent.Tool.tractor.ordinal(), tractorBeam);
            }
        });
        buttonTractorBeam.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText(tractorBeamDescription);
                return super.handle(event);
            }
        });

        int hp = 100;
        TextButton buttonAddHealth = new TextButton("Increase [" + colorItem + "]Health [" + colorCredits + "]" + costHP, VisUI.getSkin());
        buttonAddHealth.getLabel().setAlignment(Align.left);
        buttonAddHealth.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costHP) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for  hp");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //purchase
                cargo.credits -= costHP;
                creditsValue.setText(Mappers.cargo.get(player).credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));

                Mappers.health.get(player).maxHealth += hp;
            }
        });
        buttonAddHealth.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText("Increase [" + colorItem + "]HP[]\nby [" + colorCredits + "]" + hp +"[]\nCurrent: "+ Mappers.health.get(player).maxHealth);
                return super.handle(event);
            }
        });

        TextButton buttonAddThrust = new TextButton("Increase [" + colorItem + "]Thrust  [" + colorCredits + "]" + costThrust, VisUI.getSkin());
        buttonAddThrust.getLabel().setAlignment(Align.left);
        int thrust = 200;
        buttonAddThrust.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costThrust) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for  thrust");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //purchase
                cargo.credits -= costThrust;
                creditsValue.setText(Mappers.cargo.get(player).credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));

                Mappers.vehicle.get(player).thrust += thrust;
            }
        });
        buttonAddThrust.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText("Increase [" + colorItem + "]THRUST[]\nby [" + colorCredits + "]" + thrust +"[]\nCurrent: "+ Mappers.vehicle.get(player).thrust);
                return super.handle(event);
            }
        });

        TextButton buttonLaserPower = new TextButton("Increase [" + colorItem + "]LASER DMG[" + colorCredits + "]" + costDMG, VisUI.getSkin());
        buttonLaserPower.getLabel().setAlignment(Align.left);
        int damage = 200;
        buttonLaserPower.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CargoComponent cargo = Mappers.cargo.get(player);
                if (cargo.credits < costDMG) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for  lasert");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                LaserComponent laserComponent = Mappers.laser.get(player);
                if (laserComponent == null) return;
                //purchase
                cargo.credits -= costDMG;
                creditsValue.setText(Mappers.cargo.get(player).credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));

                laserComponent.damage += damage;
            }
        });
        buttonLaserPower.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                LaserComponent laserComponent = Mappers.laser.get(player);
                text.setText("Increase [" + colorItem + "]DMG[]\nby [" + colorCredits + "]"
                        + damage +"[]\nCurrent: "+ (laserComponent == null ? 0 : laserComponent.damage));
                return super.handle(event);
            }
        });

        TextButton buttonDebugGiveMoney = new TextButton("[" + colorItem + "]DEBUG: []ADD CREDITS", VisUI.getSkin());
        buttonDebugGiveMoney.getLabel().setAlignment(Align.left);
        buttonDebugGiveMoney.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CargoComponent cargo = Mappers.cargo.get(player);
                cargo.credits += 999999;
                creditsValue.setText(Mappers.cargo.get(player).credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));

            }
        });
        buttonDebugGiveMoney.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText("cheater...");
                return super.handle(event);
            }
        });
        Table buttonTable = new VisTable();
        buttonTable.add(buttonShield).fillX().row();
        buttonTable.add(buttonLaser).fillX().row();
        buttonTable.add(buttonTractorBeam).fillX().row();
        buttonTable.add(buttonHyperDrive).fillX().row();
        buttonTable.add(buttonAddHealth).fillX().row();
        buttonTable.add(buttonAddThrust).fillX().row();
        buttonTable.add(buttonLaserPower).fillX().row();
        buttonTable.add(buttonDebugGiveMoney).fillX().row();

        Table descTable = new VisTable();
        text.setReadOnly(true);
        descTable.add(text.createCompatibleScrollPane()).growX().height(150).width(180).row();

        VisWindow window = new VisWindow("[" + titleColor + "]STATION DELTA");
        window.getTitleLabel().setAlignment(Align.center);
        window.addCloseButton();

        window.add(buttonTable).pad(10);
        window.add(descTable).pad(10).row();
        window.add(creditsTable).expandX().fillX();

        for (Actor button : buttonTable.getChildren()) {
            stage.addFocusableActor(button);
        }

        window.pack();
        stage.addActor(window);
        window.setPosition(Gdx.graphics.getWidth() / 3f, Gdx.graphics.getHeight() / 2f, Align.right);
        stage.setFocusedActor(buttonTable.getChildren().first());

        return window;
    }
}
