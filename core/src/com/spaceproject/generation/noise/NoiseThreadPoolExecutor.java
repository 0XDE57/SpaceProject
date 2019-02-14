package com.spaceproject.generation.noise;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NoiseThreadPoolExecutor extends ThreadPoolExecutor {

    private Array<NoiseGenListener> listeners;

    public NoiseThreadPoolExecutor(int numThreads) {
        super(numThreads, numThreads, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        allowCoreThreadTimeOut(true);

        listeners = new Array<NoiseGenListener>();

        Gdx.app.log(this.getClass().getSimpleName(), "NoiseThreadPool with " + getMaximumPoolSize() + " threads");
    }


    public void addListener(NoiseGenListener listener) {
        listeners.add(listener);
    }

    private void notifyListenersNoiseFinished(NoiseThread noise) {
        for (int i = 0; i < listeners.size; i++) {
            listeners.get(i).threadFinished(noise);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null) {
            notifyListenersNoiseFinished((NoiseThread)r);
        } else {
            Gdx.app.error(this.getClass().getSimpleName(), "Task failed", t);
        }
    }
    
    
    @Override
    public String toString() {
        return  "active:" + getActiveCount()
                + ", completed:" + getCompletedTaskCount()
                + ", task count:" + getTaskCount()
                + ", pool size:" + getCorePoolSize();
    }
}