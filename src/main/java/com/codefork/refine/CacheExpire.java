package com.codefork.refine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CacheExpire implements Runnable {

    private CacheManager cacheManager;
    private Log log = LogFactory.getLog(CacheExpire.class);
    private boolean keepGoing = true;

    public CacheExpire(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void run() {
        log.info("Starting cache expiration thread: waking every 60s, lifetime=" + cacheManager.getCache().getLifetime() + ", maxSize=" + cacheManager.getCache().getMaxSize());
        try {
            while(keepGoing) {
                Thread.sleep(60000);
                try {
                    cacheManager.expireCache();
                } catch(Exception e) {
                    // if we don't catch here, a possibly intermittent or edge-case error can
                    // cause the cache expire thread to die, and the cache will grow uncontrollably
                    log.error("Ignoring error that occurred in cacheManager.expireCache(): " + e.toString());
                }
            }
        } catch (InterruptedException e) {
            // noop
        }
        log.info("exiting CacheExpire");
        // break circular reference
        cacheManager = null;
    }
}
