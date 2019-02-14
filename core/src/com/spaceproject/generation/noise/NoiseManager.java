package com.spaceproject.generation.noise;

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
    
    public void generate(long seed, PlanetComponent planet){
        noiseThreadPool.execute(new NoiseThread(planet.scale, planet.octaves, planet.persistence, planet.lacunarity, seed, planet.mapSize, Tile.defaultTiles));
    }
    
    public NoiseBuffer getNoiseForSeed(long seed) {
        if (loadedNoise.containsKey(seed)) {
            return loadedNoise.get(seed);
        }
        return null;
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
