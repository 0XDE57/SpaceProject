package com.spaceproject.generation.noise;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class NoiseManager {
    
    //todo: move stuff out of gamescreen, let this become the master
    public static LinkedBlockingQueue<NoiseBuffer> noiseBufferQueue;
    public static NoiseThreadPoolExecutor noiseThreadPool;
    public HashMap<Long, NoiseBuffer> loadedNoise = new HashMap<Long, NoiseBuffer>();
}
