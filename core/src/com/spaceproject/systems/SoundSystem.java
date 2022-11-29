package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

public class SoundSystem extends EntitySystem implements Disposable {
    
    Sound kick;
    Sound synth;
    Sound f3;
    long kickID;
    long synthID;
    long f3ID;
    
    @Override
    public void addedToEngine(Engine engine) {
        //  Supported Formats: MP3, OGG and WAV
        //  - WAV files are quite large compared to other formats
        //      - com.badlogic.gdx.utils.GdxRuntimeException: WAV files must have 16 bits per sample: 8
        //  - OGG files don’t work on RoboVM (iOS) nor with Safari (GWT)
        //  - MP3 files have issues with seamless looping.
        //  The current upper limit for decoded audio is 1 MB.
        //  https://libgdx.com/wiki/audio/sound-effects
        
        // Latency is not great and the default implementation is not recommended for latency sensitive apps like rhythm games.
        // https://libgdx.com/wiki/audio/audio#audio-on-android
        
        //load sounds (should use assetmanager?)
        kick = Gdx.audio.newSound(Gdx.files.internal("sound/laserShoot.wav"));
        synth = Gdx.audio.newSound(Gdx.files.internal("sound/synth.mp3"));
        f3 = Gdx.audio.newSound(Gdx.files.internal("sound/f3.mp3"));
     
        //-6 to -12 db track rendering because headroom
        
        //given the physics based nature of this game, id say rhythm is certainly important given we want collisions to feel accurate
        //12 notes
        //how many octaves can we squeeze out of 1 MB?
        //
    }
    
    @Override
    public void update(float deltaTime) {
        //todo: should be event based, so any system can call
        // should no be checking mouse here as controller can activate too also menus.
        //see: https://github.com/libgdx/ashley/blob/master/ashley/tests/com/badlogic/ashley/signals/SignalTests.java
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            try {
                kickID = kick.play(); // play new sound and keep handle for further manipulation
                kick.setPitch(kickID, MathUtils.random(0.5f, 2.0f));//0.5 - 2.0
                
                //for continuous sound (loop), raw sound file
                // - no gaps
                // - wave should line up to prevent clipping
                //synthID = synth.loop();
                //synth.setPitch(synthID, 0.5f);
            } catch (Exception e) {
                Gdx.app.error(this.getClass().getSimpleName(), "", e);
            }
        }
        
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
            synth.stop();
        }
        
        shatterPerFrame = 0;//reset
    }
    
    int shatterPerFrame = 0;
    int shatterCount = 0;
    public void asteroidShatter() {
        Gdx.app.debug(this.getClass().getSimpleName(), "" + shatterCount);
        shatterCount++;
        shatterPerFrame++;
        if (shatterPerFrame > 1) {
            //Gdx.app.debug(this.getClass().getSimpleName(), "" + shatterCount);
            //return;
        }
        
        f3ID = f3.play(); // play new sound and keep handle for further manipulation
        f3.setPitch(f3ID, MathUtils.random(0.5f, 2.0f));//0.5 - 2.0
    }
    
    @Override
    public void dispose() {
        f3.dispose();
        kick.dispose();
    }
    
}
