package com.spaceproject.ui.menu.tabs;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.DebugConfig;

public class DebugTab extends HotKeyTab {
    
    public DebugTab() {
        super("debug", Input.Keys.F4);
        final DebugConfig debugCFG = SpaceProject.configManager.getConfig(DebugConfig.class);

        final VisCheckBox toggleInfiniteFire = new VisCheckBox("infinite fire!".toUpperCase(), debugCFG.infiniteFire);
        toggleInfiniteFire.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.infiniteFire = toggleInfiniteFire.isChecked();
            }
        });

        final VisCheckBox toggleAsteroidSpawn = new VisCheckBox("spawn asteroid on [right-click]", debugCFG.spawnAsteroid);
        toggleAsteroidSpawn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.spawnAsteroid = toggleAsteroidSpawn.isChecked();
            }
        });

        final VisCheckBox toggleCameraLerp = new VisCheckBox("lerp camera", debugCFG.spawnAsteroid);
        toggleCameraLerp.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.lerpCam = toggleCameraLerp.isChecked();
            }
        });

        final VisCheckBox toggleComponentList = new VisCheckBox("show components", debugCFG.drawComponentList);
        toggleComponentList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawComponentList = toggleComponentList.isChecked();
            }
        });
        
        
        final VisCheckBox togglePos = new VisCheckBox("show pos", debugCFG.drawPos);
        togglePos.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawPos = togglePos.isChecked();
            }
        });
        
        
        final VisCheckBox toggleB2Debug = new VisCheckBox("show box2d debug", debugCFG.box2DDebugRender);
        toggleB2Debug.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.box2DDebugRender = toggleB2Debug.isChecked();
            }
        });

        final VisCheckBox toggleBodies = new VisCheckBox("bodies", debugCFG.drawBodies);
        toggleBodies.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawBodies = toggleBodies.isChecked();
            }
        });
        final VisCheckBox toggleInactiveBodies = new VisCheckBox("inactive bodies", debugCFG.drawInactiveBodies);
        toggleInactiveBodies.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawInactiveBodies = toggleInactiveBodies.isChecked();
            }
        });
        final VisCheckBox toggleAABB = new VisCheckBox("AABB", debugCFG.drawAABBs);
        toggleAABB.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawAABBs = toggleAABB.isChecked();
            }
        });
        final VisCheckBox toggleContacts = new VisCheckBox("contacts", debugCFG.drawContacts);
        toggleContacts.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawContacts = toggleContacts.isChecked();
            }
        });
        final VisCheckBox toggleJoints = new VisCheckBox("joints", debugCFG.drawJoints);
        toggleJoints.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawJoints = toggleJoints.isChecked();
            }
        });
        
        final VisCheckBox toggleVectors = new VisCheckBox("velocity vectors", debugCFG.drawVelocities);
        toggleVectors.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawVelocities = toggleVectors.isChecked();
            }
        });

        final VisCheckBox toggleOrbitPath = new VisCheckBox("orbit path", debugCFG.drawOrbitPath);
        toggleOrbitPath.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawOrbitPath = toggleOrbitPath.isChecked();
            }
        });
        
        
        final VisCheckBox toggleMousePos = new VisCheckBox("show mouse pos", debugCFG.drawMousePos);
        toggleMousePos.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawMousePos = toggleMousePos.isChecked();
            }
        });

        
        final VisCheckBox toggleFPS = new VisCheckBox("show fps", debugCFG.drawFPS);
        toggleFPS.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawFPS = toggleFPS.isChecked();
            }
        });
        
        final VisCheckBox toggleExtraInfo = new VisCheckBox("show diagnostic info", debugCFG.drawDiagnosticInfo);
        toggleExtraInfo.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawDiagnosticInfo = toggleExtraInfo.isChecked();
            }
        });
        
        final VisCheckBox toggleEntityList = new VisCheckBox("show entity list", debugCFG.drawEntityList);
        toggleEntityList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawEntityList = toggleEntityList.isChecked();
            }
        });
        

        getContentTable().add(toggleInfiniteFire).left().row();
        getContentTable().add(toggleAsteroidSpawn).left().row();
        getContentTable().add(toggleCameraLerp).left().row();
        getContentTable().add(toggleFPS).left();
        getContentTable().add(toggleExtraInfo).left().row();
        getContentTable().add(toggleMousePos).left().row();
        getContentTable().add(toggleEntityList).left().row();
        getContentTable().add(togglePos).left();
        getContentTable().add(toggleComponentList).left().row();
        getContentTable().add(toggleOrbitPath).left().row();
        getContentTable().add(toggleB2Debug).left().row();
        getContentTable().add(toggleBodies).left();
        getContentTable().add(toggleInactiveBodies).left();
        getContentTable().add(toggleAABB).left().row();
        getContentTable().add(toggleJoints).left().row();
        getContentTable().add(toggleContacts).left().row();
        getContentTable().add(toggleVectors).left().row();
    }
    
}
