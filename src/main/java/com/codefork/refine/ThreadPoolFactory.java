package com.codefork.refine;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ThreadPoolFactory {

    private Map<String, ThreadPool> sharedThreadPools = new HashMap<>();

    public ThreadPool createThreadPool() {
        return new ThreadPool();
    }

    public ThreadPool getSharedThreadPool(String key) {
        if(!sharedThreadPools.containsKey(key)) {
            sharedThreadPools.put(key, new ThreadPool());
        }
        return sharedThreadPools.get(key);
    }

    public void releaseThreadPool(ThreadPool pool) {
        for(ThreadPool p: sharedThreadPools.values()) {
            if(p.equals(pool)) {
                // we never shut down shared thread pools.
                // TODO: keep a count of how many things use
                // a shared thread pool and release when 0.
                return;
            }
        }
        pool.shutdown();
    }

}
