package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.SoundComponent;

public class SoundSystem extends EntitySystem implements Disposable {
    
    //  todo: should be event based?, so any system can call
    //  currently any systems wanting to make a sound can get the system from the engine, but inter-system coupling.
    //  see: https://github.com/libgdx/ashley/blob/master/ashley/tests/com/badlogic/ashley/signals/SignalTests.java
    
    //  The current upper limit for decoded audio is 1 MB.
    //  https://libgdx.com/wiki/audio/sound-effects
    //
    //  Supported Formats: MP3, OGG and WAV
    //  - WAV files are quite large compared to other formats
    //      - WAV files must have 16 bits per sample
    //  - OGG files don’t work on RoboVM (iOS) nor with Safari (GWT)
    //  - MP3 files have issues with seamless looping.
    //
    //  Continuous / looping sound: soundID = sound.loop();
    //  - raw sound file must have no gaps
    //  - start and end of wave should line up to prevent clipping
    //  - must not use mp3
    //
    //  Panning:
    //  - sounds must be MONO
    //
    //  Latency is not great and the default implementation is not recommended for latency sensitive apps like rhythm games.
    //  https://libgdx.com/wiki/audio/audio#audio-on-android
    //  Given the physics based nature of this game, id say rhythm is certainly important given we want collisions to feel accurate
    
    //  Given the above sound files will be expected as:
    //  16-bit WAV -> MONO
    //  Roughly -3 to -6 db track rendering?
    
    private float masterVolume = 1.0f;
    private boolean soundEnabled = true;
    
    static ArrayMap<Long, Sound> activeLoops;
    AssetManager assetManager;
    
    Sound shipEngineActiveLoop, shipEngineAmbientLoop;
    Sound f3;
    Sound laserShoot, laserShootCharge;
    Sound hullImpact, hullImpactHeavy;
    Sound shieldImpact;
    Sound shieldCharge, shieldOn, shieldOff, shieldAmbientLoop;
    Sound hyperdriveEngage;
    Sound pickup;
    
    @Override
    public void addedToEngine(Engine engine) {
        activeLoops = new ArrayMap<>();
        
        /*
        //todo: should use assetmanager?
        assetManager = new AssetManager();
        assetManager.load("sound/brownNoise.wav", Sound.class);
        assetManager.finishLoading();
        shipEngineActiveLoop = assetManager.get("sound/55hz.wav", Sound.class);//fails: wrong path?
         */
        
        //load sounds
        shipEngineActiveLoop = Gdx.audio.newSound(Gdx.files.internal("sound/brownNoise.wav"));
        shipEngineAmbientLoop = Gdx.audio.newSound(Gdx.files.internal("sound/55hz.wav"));
        
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
        
        pickup = Gdx.audio.newSound(Gdx.files.internal("sound/pickup.wav"));
    }
    
    public static void stopSound(SoundComponent soundComponent) {
        Sound sound = activeLoops.get(soundComponent.soundID);
        if (sound != null) {
            sound.stop(soundComponent.soundID);
        }
        activeLoops.removeKey(soundComponent.soundID);
        soundComponent.soundID = -1;
        soundComponent.active = false;
    }
    
    boolean isEngineLooping = false;
    public long shipEngineActive(boolean startLoop, float pitch) {
        //if (!soundEnabled) return -1;
        
        long shipEngineActiveID = -1;
        if (startLoop) {
            if (!isEngineLooping) {
                shipEngineActiveID = shipEngineActiveLoop.loop(1, pitch, 0);
                //activeLoops.put(shipEngineActiveID, shipEngineActiveLoop);
            }
            isEngineLooping = true;
            //shipEngineActiveLoop.setPitch(shipEngineActiveID, pitch);
        } else {
            isEngineLooping = false;
            shipEngineActiveLoop.stop();
        }
        return shipEngineActiveID;
    }
    
    float accumulator;
    public long shipEngineAmbient(SoundComponent sound, boolean active, float velocity, float delta) {
        if (active) {
            if (sound.soundID == -1) {
                sound.soundID = shipEngineAmbientLoop.loop();
                sound.active = true;
                activeLoops.put(sound.soundID, shipEngineAmbientLoop);
            }
            //todo sound id of 0 seems to not play?
            //if (sound.soundID == 0) { Gdx.app.error(getClass().getSimpleName(), sound.soundID + ""); }
            
            //pitch
            float relVel = velocity / Box2DPhysicsSystem.getVelocityLimit();
            float pitch = MathUtils.map(0f, 1f, 0.5f, 2.0f, relVel);
            shipEngineAmbientLoop.setPitch(sound.soundID, pitch * masterVolume);
            //volume
            accumulator += 30.0f * relVel * delta;
            float oscillator = (float) Math.abs(Math.sin(accumulator));//todo move to sound component
            shipEngineAmbientLoop.setVolume(sound.soundID, oscillator);
        } else {
            if (sound.soundID != -1) {
                shipEngineAmbientLoop.setLooping(sound.soundID, false);
                activeLoops.removeKey(sound.soundID);
                sound.soundID = -1;
                sound.active = false;
            }
        }
        return sound.soundID;
    }
    
    boolean isShieldLoop = false;
    public long shieldAmbient(boolean startLoop) {
        long shieldAmbientID = -1;
        if (startLoop) {
            if (!isShieldLoop) {
                shieldAmbientID = shieldAmbientLoop.play();
            }
            isShieldLoop = true;
        } else {
            isShieldLoop = false;
            shieldAmbientLoop.stop();
        }
        shieldAmbientLoop.setLooping(shieldAmbientID, startLoop);
        return shieldAmbientID;
    }
    
    public long asteroidShatter() {
        float pitch = MathUtils.random(0.5f, 2.0f);
        return f3.play(0.25f, pitch, 0);
    }
    
    int curStep = 0;
    public long ascendingTone() {
        //  play new sound and keep handle for further manipulation
        //  range -> 0.5 - 2.0 = half to double frequency
        //  +/-1 octave from sample frequency
        //  lower octave = 0.5 - 1.0 (eg: 440hz * 0.5 = 220)
        //  upper octave = 1.0 - 2.0 (eg: 440hz * 2.0 = 880)
        //  lower: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,11
        //  upper: 12,13,14,15,16,17,18,19,20,21,22,24
        
        //upper octave = 1.0 - 2.0 (eg: 440hz * 2.0 = 880)
        //curStep 12 = 1.0 + ((1/12) * 0)  = 1.0;
        //curStep 24 = 1.0 + ((1/12) * 12) = 2.0;
        float step = 1.0f/12.0f; // = 0.08333
        float pitch = 1.0f + (step * (curStep-12));
        if (curStep < 12) {
            //lower octave = 0.5 - <1.0 (eg: 440hz * 0.5 = 220)
            //curStep 0  = 0.5 + ((1/12) * 0)  = 0.5;
            //curStep 11 = 0.5 + ((1/12) * 11) = 0.916; (0.916 / 2) + 0.5f = 0.9583?;
            //curStep 12 = 0.5 + ((1/12) * 12) = 1.5;
            step *= 0.5f;//scale down 1.0 -> 0.5
            pitch = 0.5f + (step * curStep);
        }
        //Gdx.app.debug(getClass().getSimpleName(), curStep + ": " + step + " -> " + pitch);
        
        //increase step but loop back
        if (curStep++ > 24) {
            curStep = 0;
        }
        
        return f3.play(0.25f, pitch, 0);
    }
    
    public void laserShoot() {
        laserShoot(0.3f, 1 + MathUtils.random(-0.02f, 0.02f));
    }
    
    public long laserShoot(float volume, float pitch) {
        return laserShoot.play(volume, pitch, 0);
    }
    
    public long laserCharge(float volume, float pitch) {
        return laserShootCharge.play(volume, pitch, 0);
    }
    
    public long hullImpactLight(float volume) {
        return hullImpact.play(volume, 2, 0);
    }
    
    public long hullImpactHeavy(float pitch) {
        return hullImpactHeavy.play(1, pitch, 0);
    }
    
    public long shieldImpact(float volume) {
        return shieldImpact.play(volume, 1, 0);
    }
    
    public long shieldCharge() {
        return shieldCharge.play();
    }
    
    public long shieldOn() {
        return shieldOn.play();
    }
    
    public long shieldOff() {
        return shieldOff.play();
    }
    
    public long hyperdriveEngage() {
        return 0;//disable for now
        //return hyperdriveEngageID = hyperdriveEngage.play();
    }
    
    public long pickup() {
        float pitch = MathUtils.random(0.5f, 2.0f);
        return pickup.play(0.5f, pitch, 0);
    }
    
    @Override
    public void dispose() {
        shipEngineActiveLoop.dispose();
        shipEngineAmbientLoop.dispose();
        f3.dispose();
        laserShoot.dispose();
        laserShootCharge.dispose();
        hullImpact.dispose();
        hullImpactHeavy.dispose();
        shieldImpact.dispose();
        shieldOn.dispose();
        shieldOff.dispose();
        shieldAmbientLoop.dispose();
        pickup.dispose();
    }
    
}
