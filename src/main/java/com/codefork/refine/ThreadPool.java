package com.codefork.refine;

import com.codefork.refine.datasource.SearchTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around an ExecutorService thread pool.
 */
public class ThreadPool {

    /**
     * NOTE: VIAF seems to have a limit of 6 simultaneous
     * requests. To be conservative, we default to 4 for
     * the entire app.
     */
    public static final int INITIAL_POOL_SIZE = 4;

    // shrink rapidly, but grow slowly
    private long waitPeriodBeforeShrinkingMs = 30000; // 30s
    private long waitPeriodBeforeGrowingMs = 600000; // 10 mins
    private long waitPeriodBeforeResetMs = 3600000; // 1 hour

    private Log log = LogFactory.getLog(ThreadPool.class);

    private ThreadPoolExecutor executor;
    private long lastTimePoolAdjusted = 0;

    public ThreadPool() {
        // TODO: make thread pool size configurable
        log.info("Starting thread pool, size = " + INITIAL_POOL_SIZE);
        executor = new ThreadPoolExecutor(INITIAL_POOL_SIZE, INITIAL_POOL_SIZE, 0, TimeUnit.HOURS, new LinkedBlockingQueue<Runnable>());
    }

    public ThreadPool(long waitPeriodBeforeShrinkingMs,
                      long waitPeriodBeforeGrowingMs,
                      long waitPeriodBeforeResetMs) {
        this();
        this.waitPeriodBeforeShrinkingMs = waitPeriodBeforeShrinkingMs;
        this.waitPeriodBeforeGrowingMs = waitPeriodBeforeGrowingMs;
        this.waitPeriodBeforeResetMs = waitPeriodBeforeResetMs;
    }

    /**
     * Submit a task to the pool, returning a Future immediately.
     * Also tries to grow the pool, if necessary.
     * @param task
     * @return
     */
    public Future<SearchResult> submit(SearchTask task) {
        if(lastTimePoolAdjusted != 0) {
            grow();
            long now = System.currentTimeMillis();
            if(now - lastTimePoolAdjusted > waitPeriodBeforeResetMs) {
                // reset this var, to prevent submit() from trying to grow back
                lastTimePoolAdjusted = 0;
            }
        }
        return executor.submit(task);
    }

    /**
     * @return current size of the thread pool
     */
    public int getPoolSize() {
        return executor.getCorePoolSize();
    }

    private void setPoolSize(int newSize) {
        executor.setCorePoolSize(newSize);
        executor.setMaximumPoolSize(newSize);
    }

    /**
     * Shrink the size of the pool, if we can.
     */
    public synchronized void shrink() {
        int size = getPoolSize();
        if(size > 1) {
            long now = System.currentTimeMillis();
            // we need a bit of time for the last operation to take effect
            // before we try to shrink again
            if(now - lastTimePoolAdjusted > waitPeriodBeforeShrinkingMs) {
                int newSize = size - 1;
                log.info("Shrinking pool, new size = " + newSize);
                setPoolSize(newSize);
                lastTimePoolAdjusted = now;
            }
        }
    }

    /**
     * Grow the size of the pool back up to the initial size, if we can.
     */
    public synchronized void grow() {
        int size = getPoolSize();
        if(size < INITIAL_POOL_SIZE) {
            long now = System.currentTimeMillis();
            // grow slowly
            if(now - lastTimePoolAdjusted > waitPeriodBeforeGrowingMs) {
                int newSize = size + 1;
                log.info("Growing pool, new size = " + newSize);
                setPoolSize(newSize);
                lastTimePoolAdjusted = now;
            }
        }
    }

    public void shutdown() {
        log.info("Shutting down thread pool");
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            log.error("Executor was interrupted while awaiting termination: " + e);
        }
    }

}
