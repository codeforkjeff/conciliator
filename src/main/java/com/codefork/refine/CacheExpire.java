package com.codefork.refine;

import com.codefork.refine.viaf.VIAF;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CacheExpire implements Runnable {

    private final VIAF viaf;
    private Log log = LogFactory.getLog(CacheExpire.class);
    private boolean keepGoing = true;

    /**
     * Keeps a reference to VIAF object b/c its cache object
     * changes, so we have to avoid holding on to a Cache instance
     * in this class.
     * @param viaf
     */
    public CacheExpire(VIAF viaf) {
        this.viaf = viaf;
    }

    public void stopGracefully() {
        keepGoing = false;
    }

    @Override
    public void run() {
        log.info("Starting cache expiration thread: waking every 60s, lifetime=" + viaf.getCacheLifetime() + ", maxSize=" + viaf.getCacheMaxSize());
        try {
            while(keepGoing) {
                Thread.sleep(60000);
                viaf.expireCache();
            }
        } catch (InterruptedException e) {
            log.info("CacheExpire interrupted");
        }
        log.info("exiting CacheExpire");
    }
}
