package com.codefork.refine;

import com.codefork.refine.resources.Result;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Takes care of the Cache, including the thread that expires it periodically.
 * We need this because the Cache object gets replaced when it expires,
 * so there needs to be this intermediate object for synchronizing access
 * to the Cache object.
 */
public class CacheManager {

    Log log = LogFactory.getLog(CacheManager.class);

    private Cache<String, List<Result>> cache;
    private final Object cacheLock = new Object();
    private CacheExpire cacheExpire;
    private Thread thread;
    private String name;

    public CacheManager(String name) {
        this.name = name;
        this.cache = new Cache<>(getName());
    }

    public String getName() {
        return name;
    }

    public void startExpireThread() {
        if(!isExpireThreadRunning()) {
            cacheExpire = new CacheExpire(this);

            thread = new Thread(cacheExpire);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void stopExpireThread() {
        if(isExpireThreadRunning()) {
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isExpireThreadRunning() {
        return thread != null;
    }

    public void expireCache() {
        if (cache.getCount() > 0) {
            // expire entries and replace original cache with new one
            int size = cache.getCount();
            Cache<String, List<Result>> newCache = cache.expireCache();
            synchronized (cacheLock) {
                int size2 = cache.getCount();
                if(size2 != size) {
                    log.warn("Cache grew during expiration. This happens sometimes, but shouldn't happen frequently. size diff=" + (size2 - size));
                }
                cache = newCache;
            }
        }
    }

    /**
     * Callers should never hold on to cache objects, but call getCache()
     * each time they need a reference to it within a code block.
     * @return
     */
    public Cache<String, List<Result>> getCache() {
        Cache<String, List<Result>> c;
        synchronized(cacheLock) {
            c = cache;
        }
        return c;
    }
}
