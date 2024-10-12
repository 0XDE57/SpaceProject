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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.systems.Box2DPhysicsSystem;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;


public class SpaceStationMenu {
    
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
        String tractorBeamDescription = "[" + colorItem + "]" + tractorUpgrade + "[]\npush or pull objects.\n\nHold [" + colorControl + "][" + laserControl + "][] to activate.\nDouble Tap to toggle between PUSH & PULL";
        int costHyper = 100000;
        int costShield = 25000;
        int costLaser = 30000;
        int costTractorBeam = 10000;
        int costHP = 40000;
        int costThrust = 20000;
        int costCannonDMG = 10000;
        int costCannonVelocity = 10000;
        //int costCannonCooldown = 20000;//todo
        int costLaserDMG = 20000;
        int costLaserRange = 20000;
        //int costTractorForce = 10000;//todo
        //int costTractorRange = 10000;//todo
        //upgrade values
        int health = 100;
        int thrust = 200;
        int cannonDamage = 5;
        float cannonVelocity = 20;
        int laserDamage = 30;
        float laserRange = 20;

        ScrollableTextArea text = new ScrollableTextArea("");
        text.removeListener(text.getDefaultInputListener());
        VisTable creditsTable = new VisTable();
        VisLabel creditsText = new VisLabel("CREDITS: ");
        creditsTable.add(creditsText);
        CargoComponent cargo = Mappers.cargo.get(player);
        VisLabel creditsValue = new VisLabel("[" + colorCredits + "]" + cargo.credits);
        //creditsValue.addAction(forever(sequence(color(Color.RED,0.2f), color(Color.BLUE, 0.2f), delay(0.5f))));
        creditsTable.add(creditsValue);

        //hyper drive
        VisTextButton buttonHyperDrive = new VisTextButton("[" + colorItem + "]" + hyperdriveUpgrade);
        buttonHyperDrive.getLabel().setAlignment(Align.left);
        buttonHyperDrive.add(new VisLabel("[" + colorCredits + "]" + costHyper));
        buttonHyperDrive.setDisabled(Mappers.hyper.get(player) != null);
        buttonHyperDrive.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Mappers.hyper.get(player) != null) return;
                if (cargo.credits < costHyper) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for " + hyperdriveUpgrade);
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //purchase
                cargo.credits -= costHyper;
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
                //add new hyperdrive
                HyperDriveComponent hyperDrive = new HyperDriveComponent();
                hyperDrive.speed = 2000;
                hyperDrive.coolDownTimer = new SimpleTimer(2000);
                hyperDrive.chargeTimer = new SimpleTimer(2000);
                hyperDrive.graceTimer = new SimpleTimer(1000);
                player.add(hyperDrive);
                buttonHyperDrive.setDisabled(true);
            }
        });
        buttonHyperDrive.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText(hyperdriveDescription);
                return super.handle(event);
            }
        });

        //shield
        VisTextButton buttonShield = new VisTextButton("[" + colorItem + "]" + shieldUpgrade);
        buttonShield.getLabel().setAlignment(Align.left);
        buttonShield.add(new VisLabel("[" + colorCredits + "]" + costShield));
        buttonShield.setDisabled(Mappers.shield.get(player) != null);
        VehicleComponent vehicle = Mappers.vehicle.get(player);
        buttonShield.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Mappers.shield.get(player) != null) return;
                if (cargo.credits < costShield) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for " + shieldUpgrade);
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
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
                //purchase
                cargo.credits -= costShield;
                //update UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
                buttonShield.setDisabled(true);
            }
        });
        buttonShield.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText(shieldDescription);
                return super.handle(event);
            }
        });

        //laser
        VisTextButton buttonLaserPower = new VisTextButton("Increase [" + colorItem + "]Laser Damage");
        VisTextButton buttonLaserRange = new VisTextButton("Increase [" + colorItem + "]Laser Range");
        VisTextButton buttonLaser = new VisTextButton("[" + colorItem + "]" + laserUpgrade);
        buttonLaser.getLabel().setAlignment(Align.left);
        buttonLaser.add(new VisLabel("[" + colorCredits + "]" + costLaser));
        buttonLaser.setDisabled(getLaserComponent(player, vehicle) != null);
        buttonLaser.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (getLaserComponent(player, vehicle) != null) return;
                if (cargo.credits < costLaser) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for " + laserUpgrade);
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                int referenceWavelength = 589;//"yellow doublet" sodium D line
                LaserComponent laser = new LaserComponent(520, 250, 30, 1);
                vehicle.tools.put(VehicleComponent.Tool.laser.ordinal(), laser);
                //purchase
                cargo.credits -= costLaser;
                //update UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
                buttonLaser.setDisabled(true);
                buttonLaserPower.setDisabled(false);
                buttonLaserRange.setDisabled(false);
            }
        });
        buttonLaser.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText(laserDescription);
                return super.handle(event);
            }
        });

        // laser damage
        buttonLaserPower.getLabel().setAlignment(Align.left);
        buttonLaserPower.add(new VisLabel("[" + colorCredits + "]" + costLaserDMG));
        buttonLaserPower.setDisabled(getLaserComponent(player, vehicle) == null);
        buttonLaserPower.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (cargo.credits < costLaserDMG) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for  laser");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //upgrade
                LaserComponent laser = getLaserComponent(player, vehicle);
                if (laser == null) return;
                laser.damage += laserDamage;
                //purchase
                cargo.credits -= costLaserDMG;
                //update UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
            }
        });
        buttonLaserPower.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                LaserComponent laser = getLaserComponent(player, vehicle);
                text.setText("Increase [" + colorItem + "]DMG\nby: [" + colorCredits + "]" + laserDamage +"\nCurrent: [" + colorCredits + "]"+ (laser == null ? "N/A" : laser.damage));
                return super.handle(event);
            }
        });

        // laser range
        buttonLaserRange.getLabel().setAlignment(Align.left);
        buttonLaserRange.add(new VisLabel("[" + colorCredits + "]" + costLaserRange));
        buttonLaserRange.setDisabled(getLaserComponent(player, vehicle) == null);
        buttonLaserRange.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (cargo.credits < costLaserRange) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for laser range");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //upgrade
                LaserComponent laser = getLaserComponent(player, vehicle);
                if (laser == null) return;
                laser.maxDist += laserRange;
                //purchase
                cargo.credits -= costLaserRange;
                //update UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
            }
        });
        buttonLaserRange.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                LaserComponent laser = getLaserComponent(player, vehicle);
                text.setText("Increase [" + colorItem + "]Range\nby: [" + colorCredits + "]" + laserRange +"\nCurrent: [" + colorCredits + "]"+ (laser == null ? "N/A" : laser.maxDist));
                return super.handle(event);
            }
        });

        //tractor beam
        VisTextButton buttonTractorBeam = new VisTextButton("[" + colorItem + "]" + tractorUpgrade);
        buttonTractorBeam.getLabel().setAlignment(Align.left);
        buttonTractorBeam.add(new VisLabel("[" + colorCredits + "]" + costTractorBeam));
        buttonTractorBeam.setDisabled(getTractorComponent(player, vehicle) != null);
        buttonTractorBeam.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (getTractorComponent(player, vehicle) != null) return;
                if (cargo.credits < costTractorBeam) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for " + tractorUpgrade);
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //add upgrade to tools inventory
                TractorBeamComponent tractorBeam = new TractorBeamComponent();
                tractorBeam.maxDist = 200;
                tractorBeam.magnitude = 70000;
                vehicle.tools.put(VehicleComponent.Tool.tractor.ordinal(), tractorBeam);
                //purchase
                cargo.credits -= costTractorBeam;
                //update UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
                buttonTractorBeam.setDisabled(true);
            }
        });
        buttonTractorBeam.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText(tractorBeamDescription);
                return super.handle(event);
            }
        });

        // health
        VisTextButton buttonAddHealth = new VisTextButton("Increase [" + colorItem + "]Health");
        buttonAddHealth.getLabel().setAlignment(Align.left);
        buttonAddHealth.add(new VisLabel("[" + colorCredits + "]" + costHP));
        buttonAddHealth.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (cargo.credits < costHP) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for  hp");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //upgrade
                Mappers.health.get(player).maxHealth += health;
                //purchase
                cargo.credits -= costHP;
                //update UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
            }
        });
        buttonAddHealth.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText("Increase [" + colorItem + "]HP[]\nby: [" + colorCredits + "]" + health +"\nCurrent:  [" + colorCredits + "]"+ Mappers.health.get(player).maxHealth);
                return super.handle(event);
            }
        });

        //thrust
        VisTextButton buttonAddThrust = new VisTextButton("Increase [" + colorItem + "]Thrust");
        buttonAddThrust.getLabel().setAlignment(Align.left);
        buttonAddThrust.add(new VisLabel("[" + colorCredits + "]" + costThrust));
        buttonAddThrust.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (cargo.credits < costThrust) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for  thrust");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //upgrade
                Mappers.vehicle.get(player).thrust += thrust;
                //purchase
                cargo.credits -= costThrust;
                //update UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
            }
        });
        buttonAddThrust.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText("Increase [" + colorItem + "]THRUST\nby: [" + colorCredits + "]" + thrust +"[]\nCurrent: [" + colorCredits + "]"+ Mappers.vehicle.get(player).thrust);
                return super.handle(event);
            }
        });

        // Cannon DMG
        VisTextButton buttonAddCannonDamage = new VisTextButton("Increase [" + colorItem + "]Cannon Damage ");
        buttonAddCannonDamage.getLabel().setAlignment(Align.left);
        buttonAddCannonDamage.add(new VisLabel("[" + colorCredits + "]" + costCannonDMG));
        buttonAddCannonDamage.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (cargo.credits < costCannonDMG) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for cannon damage");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                //upgrade
                CannonComponent cannon = getCannonComponent(player, vehicle);
                cannon.damage += cannonDamage;
                //purchase
                cargo.credits -= costCannonDMG;
                //uddate UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
            }
        });
        buttonAddCannonDamage.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                CannonComponent cannon = getCannonComponent(player, vehicle);
                text.setText("Increase [" + colorItem + "]Cannon Damage\nby: [" + colorCredits + "]" + cannonDamage +"\nCurrent: [" + colorCredits + "]" + cannon.damage);
                return super.handle(event);
            }
        });

        // Cannon Velocity
        VisTextButton buttonAddCannonVelocity = new VisTextButton("Increase [" + colorItem + "]Cannon Velocity ");
        CannonComponent cannon = getCannonComponent(player, vehicle);
        buttonAddCannonVelocity.setDisabled(cannon != null && cannon.velocity >= Box2DPhysicsSystem.getVelocityLimit());
        buttonAddCannonVelocity.getLabel().setAlignment(Align.left);
        buttonAddCannonVelocity.add(new VisLabel("[" + colorCredits + "]" + costCannonVelocity));
        buttonAddCannonVelocity.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (cargo.credits < costCannonVelocity) {
                    //error soundfx if not enough moneys
                    Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for cannon velocity");
                    creditsValue.addAction(sequence(color(Color.RED),color(Color.WHITE, 0.5f)));
                    return;
                }
                CannonComponent cannon = getCannonComponent(player, vehicle);
                if (cannon.velocity >= Box2DPhysicsSystem.getVelocityLimit()) {
                    Gdx.app.debug(getClass().getSimpleName(), "max velocity!");
                    buttonAddCannonVelocity.setDisabled(true);
                    return;
                }
                cannon.velocity += cannonVelocity;
                cannon.velocity = Math.min(cannon.velocity, Box2DPhysicsSystem.getVelocityLimit());
                //purchase
                cargo.credits -= costCannonVelocity;
                //update UI
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)),color(Color.valueOf(colorCredits), 1f)));
            }
        });
        buttonAddCannonVelocity.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                CannonComponent cannon = getCannonComponent(player, vehicle);
                text.setText("Increase [" + colorItem + "]Cannon Velocity\nby: [" + colorCredits + "]" + cannonVelocity +"\nCurrent: [" + colorCredits + "]" + cannon.velocity);
                return super.handle(event);
            }
        });

        //debug
        VisTextButton buttonDebugGiveMoney = new VisTextButton("[#ff0000]DEBUG: []ADD CREDITS");
        buttonDebugGiveMoney.getLabel().setAlignment(Align.left);
        buttonDebugGiveMoney.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cargo.credits += 999999;
                creditsValue.setText(cargo.credits);
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

        VisTable buttonTable = new VisTable();
        buttonTable.add(buttonShield).fillX().row();
        buttonTable.add(buttonLaser).fillX().row();
        buttonTable.add(buttonTractorBeam).fillX().row();
        buttonTable.add(buttonHyperDrive).fillX().row();
        buttonTable.add(buttonAddHealth).fillX().row();
        buttonTable.add(buttonAddThrust).fillX().row();
        buttonTable.add(buttonAddCannonDamage).fillX().row();
        buttonTable.add(buttonAddCannonVelocity).fillX().row();
        buttonTable.add(buttonLaserPower).fillX().row();
        buttonTable.add(buttonLaserRange).fillX().row();
        buttonTable.add(buttonDebugGiveMoney).fillX().row();

        VisTable descTable = new VisTable();
        text.setReadOnly(true);
        descTable.add(text.createCompatibleScrollPane()).growX().height(150).width(180).row();

        VisWindow window = new VisWindow("[" + titleColor + "]STATION DELTA [E]");
        window.getTitleLabel().setAlignment(Align.center);
        window.addCloseButton();
        window.add(new Separator()).fillX().colspan(2).pad(3).row();
        window.add(buttonTable).pad(10);
        window.add(descTable).pad(10).row();
        window.add(new Separator()).fillX().colspan(2).row();
        window.add(creditsTable).colspan(2).pad(10);

        for (Actor button : buttonTable.getChildren()) {
            stage.addFocusableActor(button);
        }

        window.pack();
        stage.addActor(window);
        window.setPosition(Gdx.graphics.getWidth() / 3f, getHeight(window), Align.right);
        stage.setFocusedActor(buttonTable.getChildren().first());

        return window;
    }

    private static CannonComponent getCannonComponent(Entity player, VehicleComponent vehicle) {
        CannonComponent cannon = Mappers.cannon.get(player);
        if (cannon == null && vehicle.tools.containsKey(VehicleComponent.Tool.cannon.ordinal())) {
            return (CannonComponent) vehicle.tools.get(VehicleComponent.Tool.cannon.ordinal());
        }
        return cannon;
    }

    private static LaserComponent getLaserComponent(Entity player, VehicleComponent vehicle) {
        LaserComponent laser = Mappers.laser.get(player);
        if (laser == null && vehicle.tools.containsKey(VehicleComponent.Tool.laser.ordinal())) {
            return (LaserComponent) vehicle.tools.get(VehicleComponent.Tool.laser.ordinal());
        }
        return laser;
    }

    private static TractorBeamComponent getTractorComponent(Entity player, VehicleComponent vehicle) {
        TractorBeamComponent tractor = Mappers.tractor.get(player);
        if (tractor == null && vehicle.tools.containsKey(VehicleComponent.Tool.tractor.ordinal())) {
            return (TractorBeamComponent) vehicle.tools.get(VehicleComponent.Tool.tractor.ordinal());
        }
        return tractor;
    }

    public static float getHeight(VisWindow window) {
        return (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 3)) - 49 - window.getHeight() / 2;
    }

}
