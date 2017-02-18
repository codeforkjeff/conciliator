package com.codefork.refine.viaf;

import com.codefork.refine.ThreadPool;

/**
 * A singleton that serves as a container for a thread pool.
 * This ensures that all VIAF instances share a single thread pool
 * in the application.
 */
public class VIAFThreadPool {

    private ThreadPool threadPool;

    private VIAFThreadPool() {
        this.threadPool = new ThreadPool();
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    private static VIAFThreadPool singleton;

    public static VIAFThreadPool getSingleton() {
        synchronized(VIAFThreadPool.class) {
            if(singleton == null) {
                singleton = new VIAFThreadPool();
            }
        }
        return singleton;
    }
}
