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
                cacheManager.expireCache();
            }
        } catch (InterruptedException e) {
            // noop
        }
        log.info("exiting CacheExpire");
        // break circular reference
        cacheManager = null;
    }
}
