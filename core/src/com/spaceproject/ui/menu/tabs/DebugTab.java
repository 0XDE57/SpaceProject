package com.spaceproject.ui.menu.tabs;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.DebugConfig;

public class DebugTab extends HotKeyTab {
    
    public DebugTab() {
        super("Debug", Input.Keys.F4);
        final DebugConfig debugCFG = SpaceProject.configManager.getConfig(DebugConfig.class);

        final CheckBox toggleAsteroidSpawn = new CheckBox("spawn asteroid on [right-click]", VisUI.getSkin());
        toggleAsteroidSpawn.setChecked(debugCFG.spawnAsteroid);
        toggleAsteroidSpawn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.spawnAsteroid = toggleAsteroidSpawn.isChecked();
            }
        });

        final CheckBox toggleComponentList = new CheckBox("show components", VisUI.getSkin());
        toggleComponentList.setChecked(debugCFG.drawComponentList);
        toggleComponentList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawComponentList = toggleComponentList.isChecked();
            }
        });
        
        
        final CheckBox togglePos = new CheckBox("show pos", VisUI.getSkin());
        togglePos.setChecked(debugCFG.drawPos);
        togglePos.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawPos = togglePos.isChecked();
            }
        });
        
        
        final CheckBox toggleBounds = new CheckBox("show box2d debug", VisUI.getSkin());
        toggleBounds.setChecked(debugCFG.box2DDebugRender);
        toggleBounds.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.box2DDebugRender = toggleBounds.isChecked();
            }
        });
        
        final CheckBox toggleOrbitPath = new CheckBox("show orbit path", VisUI.getSkin());
        toggleOrbitPath.setChecked(debugCFG.drawOrbitPath);
        toggleOrbitPath.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawOrbitPath = toggleOrbitPath.isChecked();
            }
        });
        
        
        final CheckBox toggleVectors = new CheckBox("show velocity vectors", VisUI.getSkin());
        toggleVectors.setChecked(debugCFG.drawVelocities);
        toggleVectors.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawVelocities = toggleVectors.isChecked();
            }
        });
        
        
        final CheckBox toggleMousePos = new CheckBox("show mouse pos", VisUI.getSkin());
        toggleMousePos.setChecked(debugCFG.drawMousePos);
        toggleMousePos.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawMousePos = toggleMousePos.isChecked();
            }
        });
        
        
        final CheckBox toggleFPS = new CheckBox("show fps", VisUI.getSkin());
        toggleFPS.setChecked(debugCFG.drawFPS);
        toggleFPS.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawFPS = toggleFPS.isChecked();
            }
        });
        
        final CheckBox toggleExtraInfo = new CheckBox("show diagnostic info", VisUI.getSkin());
        toggleExtraInfo.setChecked(debugCFG.drawDiagnosticInfo);
        toggleExtraInfo.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawDiagnosticInfo = toggleExtraInfo.isChecked();
            }
        });
        
        final CheckBox toggleEntityList = new CheckBox("show entity list", VisUI.getSkin());
        toggleEntityList.setChecked(debugCFG.drawEntityList);
        toggleEntityList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugCFG.drawEntityList = toggleEntityList.isChecked();
            }
        });
        

        getContentTable().add(toggleAsteroidSpawn).left().row();
        getContentTable().add(toggleFPS).left();
        getContentTable().add(toggleExtraInfo).left().row();
        getContentTable().add(toggleMousePos).left().row();
        getContentTable().add(toggleEntityList).left().row();
        getContentTable().add(togglePos).left();
        getContentTable().add(toggleComponentList).left().row();
        getContentTable().add(toggleBounds).left().row();
        getContentTable().add(toggleVectors).left().row();
        getContentTable().add(toggleOrbitPath).left().row();
    }
    
}
