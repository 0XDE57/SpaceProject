package com.spaceproject.generation.noise;

import com.badlogic.gdx.Gdx;
import com.spaceproject.Tile;
import com.spaceproject.components.PlanetComponent;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class NoiseManager implements NoiseGenListener {

    private NoiseThreadPoolExecutor noiseThreadPool;
    private LinkedBlockingQueue<NoiseBuffer> noiseBufferQueue;
    private HashMap<Long, NoiseBuffer> loadedNoise;
    
    public NoiseManager(int maxThreads) {
        noiseThreadPool = new NoiseThreadPoolExecutor(maxThreads);
        noiseThreadPool.addListener(this);
        noiseBufferQueue = new LinkedBlockingQueue<>();
        loadedNoise = new HashMap<>();
    }
    
    public void generate(long seed, PlanetComponent planet) {
        if (loadedNoise.containsKey(seed)) {
            Gdx.app.log(this.getClass().getSimpleName(), "noise for seed [" + seed + "] already exists. Ignoring.");
            return;
        }
        
        noiseThreadPool.execute(new NoiseThread(planet.scale, planet.octaves, planet.persistence, planet.lacunarity, seed, planet.mapSize, Tile.defaultTiles));
    }
    
    public NoiseBuffer getNoiseForSeed(long seed) {
        if (loadedNoise.containsKey(seed)) {
            return loadedNoise.get(seed);
        }
        return null;
    }
    
    public void loadOrCreateNoiseFor(long seed, PlanetComponent planet) {
        NoiseBuffer noiseBuffer = getNoiseForSeed(seed);
    
        if (noiseBuffer == null) {
            //Gdx.app.log(this.getClass().getSimpleName(), "no noise found, generating: " + seed);
            generate(seed, planet);
        } else {
            //push to queue for pickup by SpaceLoadingSystem
            Gdx.app.log(this.getClass().getSimpleName(), "noise found, loading: " + seed);
            noiseBufferQueue.add(noiseBuffer);
        }
    }
    
    @Override
    public void threadFinished(NoiseThread noiseThread) {
        NoiseBuffer noise = noiseThread.getNoise();
        loadedNoise.put(noise.seed, noise);
        noiseBufferQueue.add(noise);
    }
    
    
    public NoiseThreadPoolExecutor getNoiseThreadPool() {
        return noiseThreadPool;
    }
    
    
    public LinkedBlockingQueue<NoiseBuffer> getNoiseBufferQueue() {
        return noiseBufferQueue;
    }
    
    
    public HashMap<Long, NoiseBuffer> getLoadedNoise() {
        return loadedNoise;
    }
    
}
