package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Interpolation;
import com.spaceproject.utility.SimpleTimer;

public class ScreenTransitionComponent implements Component {
    
    public enum TakeOffAnimStage {
        screenEffectFadeIn,
        transition,
        sync,
        screenEffectFadeOut,
        zoomOut,
        grow,
        end;
        
        private static TakeOffAnimStage[] vals = values();
        
        public TakeOffAnimStage next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }
    
    public enum LandAnimStage {
        shrink,
        zoomIn,
        screenEffectFadeIn,
        transition,
        screenEffectFadeOut,
        pause,
        exit,
        end;
        
        private static LandAnimStage[] vals = values();
        
        public LandAnimStage next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }
    
    public LandAnimStage landStage;
    public LandAnimStage curLandStage;
    public TakeOffAnimStage takeOffStage;
    public TakeOffAnimStage curTakeOffStage;
    
    public Entity planet;
    
    public SimpleTimer timer;
    public Interpolation animInterpolation;
}
