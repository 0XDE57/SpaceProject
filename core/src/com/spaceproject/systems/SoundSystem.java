package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

public class SoundSystem extends EntitySystem implements Disposable {
    
    Sound f3;
    Sound laserShoot, laserShootCharge;
    Sound hullImpact, hullImpactHeavy;
    Sound shieldImpact;
    Sound shieldCharge, shieldOn, shieldOff, shieldAmbientLoop;
    Sound hyperdriveEngage;
    long laserSID, laserCID;
    long f3ID;
    long hullImpactID, hullImpactHeavyID;
    long shieldImpactID;
    long shieldChargeID, shieldOnID, shieldOffID, shieldAmbientID;
    long hyperdriveEngageID;
    
    @Override
    public void addedToEngine(Engine engine) {
        //  The current upper limit for decoded audio is 1 MB.
        //  https://libgdx.com/wiki/audio/sound-effects
        //
        //  Supported Formats: MP3, OGG and WAV
        //  - WAV files are quite large compared to other formats
        //      - WAV files must have 16 bits per sample
        //  - OGG files don’t work on RoboVM (iOS) nor with Safari (GWT)
        //  - MP3 files have issues with seamless looping.
        //
        //  For continuous sound (loop), raw sound file must have
        //  - no gaps
        //  - start and end of wave should line up to prevent clipping
        //  soundID = sound.loop();
        //
        //  Latency is not great and the default implementation is not recommended for latency sensitive apps like rhythm games.
        //  https://libgdx.com/wiki/audio/audio#audio-on-android
        //given the physics based nature of this game, id say rhythm is certainly important given we want collisions to feel accurate
    
        //  Given the above sound files will be expected as:
        //  WAV 16-bit MONO
        //  Roughly -3 to -6 db track rendering?
        
        //  todo: should be event based, so any system can call
        //  see: https://github.com/libgdx/ashley/blob/master/ashley/tests/com/badlogic/ashley/signals/SignalTests.java
        
        //  load sounds (should use assetmanager?)
        f3 = Gdx.audio.newSound(Gdx.files.internal("sound/f3.wav"));
        
        laserShoot = Gdx.audio.newSound(Gdx.files.internal("sound/laserShoot.wav"));// laserShootW2
        laserShootCharge = Gdx.audio.newSound(Gdx.files.internal("sound/laserChargeW.mp3"));
        
        hullImpact = Gdx.audio.newSound(Gdx.files.internal("sound/hullImpactLight.mp3"));
        hullImpactHeavy = Gdx.audio.newSound(Gdx.files.internal("sound/hullImpactHeavy.mp3"));
        
        shieldImpact = Gdx.audio.newSound(Gdx.files.internal("sound/shieldImpact.mp3"));
        //shieldCharge = Gdx.audio.newSound(Gdx.files.internal("sound/shieldChargeUp.mp3"));
        shieldOn = Gdx.audio.newSound(Gdx.files.internal("sound/shieldOn.mp3"));
        shieldOff = Gdx.audio.newSound(Gdx.files.internal("sound/shieldOff.mp3"));
        shieldAmbientLoop = Gdx.audio.newSound(Gdx.files.internal("sound/shieldAmbient.wav"));
        
        //hyperdriveEngage = Gdx.audio.newSound(Gdx.files.internal("sound/hyperdriveInit.wav"));
        //hyperDriveDisengage = Gdx.audio.newSound(Gdx.files.internal("sound/hyperCharge.wav"));
    }
    
    @Override
    public void update(float deltaTime) {}
    
    int curStep = 0;
    public long asteroidShatter() {
        // play new sound and keep handle for further manipulation
        // range -> 0.5 - 2.0 = half to double frequency
        // +/-1 octave from sample frequency
        //      lower octave = 0.5 - 1.0 (eg: 440hz * 0.5 = 220)
        //      upper octave = 1.0 - 2.0 (eg: 440hz * 2.0 = 880)
        float step = 1.0f/12.0f;
        //curStep = MathUtils.random(12);//12-tone stepped random
        float pitch = 1.0f + (step * curStep);
        pitch = MathUtils.random(0.5f, 2.0f);//pure random
        f3ID = f3.play(0.25f, pitch, 0);
        curStep++;
        if (curStep > 12) {
            curStep = 0;
        }
        return f3ID;
    }
    
    
    public void laserShoot() {
        laserShoot(0.3f, 1 + MathUtils.random(-0.02f, 0.02f));
    }
    
    public long laserShoot(float volume, float pitch) {
        return laserSID = laserShoot.play(volume, pitch, 0);
    }
    
    public long laserCharge(float volume, float pitch) {
        return laserCID = laserShootCharge.play(volume, pitch, 0);
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
    
    public long shieldCharge() {
        return shieldChargeID = shieldCharge.play();
    }
    
    public long shieldOn() {
        return shieldOnID = shieldOn.play();
    }
    
    public long shieldOff() {
        return shieldOffID = shieldOff.play();
    }
    
    boolean isAlreadyLooping = false;
    public long shieldAmbient(boolean loop) {
        //if (true) return 0;
        if (loop) {
            if (!isAlreadyLooping) {
                shieldAmbientID = shieldAmbientLoop.play(0.5f);
            }
            isAlreadyLooping = true;
        } else {
            isAlreadyLooping = false;
            shieldAmbientLoop.stop();
        }
        shieldAmbientLoop.setLooping(shieldAmbientID, loop);
        return shieldAmbientID;
    }
    
    public long hyperdriveEngage() {
        return 0;//disable for now
        
        //return hyperdriveEngageID = hyperdriveEngage.play();
    }
    
    @Override
    public void dispose() {
        f3.dispose();
        laserShoot.dispose();
        laserShootCharge.dispose();
        hullImpact.dispose();
        hullImpactHeavy.dispose();
        shieldImpact.dispose();
    }
    
}
