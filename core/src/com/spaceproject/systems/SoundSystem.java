package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

public class SoundSystem extends EntitySystem implements Disposable {
    
    Sound kick;
    Sound synth;
    Sound f3;
    Sound hullImpact, hullImpactHeavy;
    Sound shieldImpact;
    long kickID;
    long synthID;
    long f3ID;
    long hullImpactID, hullImpactHeavyID;
    long shieldImpactID;
    
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
    
        
        //-3 ? db track rendering because headroom
        //some sources say -6 to -12?
    
        //given the physics based nature of this game, id say rhythm is certainly important given we want collisions to feel accurate
        //12 notes
        //how many octaves can we squeeze out of 1 MB?
        //
    
        //for continuous sound (loop), raw sound file
        // - no gaps
        // - wave should line up to prevent clipping
        //synthID = synth.loop();
    
        //todo: should be event based, so any system can call
        //see: https://github.com/libgdx/ashley/blob/master/ashley/tests/com/badlogic/ashley/signals/SignalTests.java
        
        //load sounds (should use assetmanager?)
        kick = Gdx.audio.newSound(Gdx.files.internal("sound/laserShoot.wav"));
        synth = Gdx.audio.newSound(Gdx.files.internal("sound/synth.mp3"));
        f3 = Gdx.audio.newSound(Gdx.files.internal("sound/f3.mp3"));
        hullImpact = Gdx.audio.newSound(Gdx.files.internal("sound/hullImpactLight.mp3"));
        hullImpactHeavy = Gdx.audio.newSound(Gdx.files.internal("sound/hullImpactHeavy.mp3"));
        shieldImpact = Gdx.audio.newSound(Gdx.files.internal("sound/shieldImpact.mp3"));
    }
    
    @Override
    public void update(float deltaTime) {}

    public long asteroidShatter() {
        // play new sound and keep handle for further manipulation
        return f3ID = f3.play(0.25f, MathUtils.random(0.5f, 2.0f), 0);//0.5 - 2.0
    }
    
    public void shoot() {
        shoot(1);
    }
    public long shoot(float pitch) {
        // play sound and keep handle for further manipulation
        return kickID = kick.play(0.25f, pitch, 0);//0.5 - 2.0
    }
    
    public long hullImpactLight(float volume) {
        return hullImpactID = hullImpact.play(volume, 2, 0);
    }
    
    public long hullImpactHeavy(float pitch) {
        return hullImpactHeavyID = hullImpactHeavy.play(1, pitch, 0);
    }
    
    public long shieldImpact(float volume) {
        return shieldImpactID = shieldImpact.play(volume, 1, 0);
    }
    
    @Override
    public void dispose() {
        f3.dispose();
        kick.dispose();
        hullImpact.dispose();
        hullImpactHeavy.dispose();
        shieldImpact.dispose();
    }
    
}
