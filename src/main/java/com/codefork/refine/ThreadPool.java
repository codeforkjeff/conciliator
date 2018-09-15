package com.codefork.refine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around an ExecutorService thread pool.
 */
public class ThreadPool {

    public static final int INITIAL_POOL_SIZE = 4;

    private int initialPoolSize = INITIAL_POOL_SIZE;

    // shrink rapidly, but grow slowly
    private long waitPeriodBeforeShrinkingMs = 30000; // 30s
    private long waitPeriodBeforeGrowingMs = 600000; // 10 mins
    private long waitPeriodBeforeResetMs = 3600000; // 1 hour

    private Log log = LogFactory.getLog(ThreadPool.class);

    private ThreadPoolExecutor executor;
    private long lastTimePoolAdjusted = 0;

    public ThreadPool() {
        start();
    }

    public ThreadPool(int initialSize) {
        this.initialPoolSize = initialSize;
        start();
    }

    public ThreadPool(long waitPeriodBeforeShrinkingMs,
                      long waitPeriodBeforeGrowingMs,
                      long waitPeriodBeforeResetMs) {
        this(INITIAL_POOL_SIZE, waitPeriodBeforeShrinkingMs,
                waitPeriodBeforeGrowingMs, waitPeriodBeforeResetMs);
    }

    public ThreadPool(int initialSize,
                      long waitPeriodBeforeShrinkingMs,
                      long waitPeriodBeforeGrowingMs,
                      long waitPeriodBeforeResetMs) {
        this(initialSize);
        this.waitPeriodBeforeShrinkingMs = waitPeriodBeforeShrinkingMs;
        this.waitPeriodBeforeGrowingMs = waitPeriodBeforeGrowingMs;
        this.waitPeriodBeforeResetMs = waitPeriodBeforeResetMs;
    }

    public void start() {
        if(executor == null || executor.isShutdown()) {
            log.info("Starting thread pool, size = " + initialPoolSize);
            executor = new ThreadPoolExecutor(initialPoolSize, initialPoolSize, 0, TimeUnit.HOURS, new LinkedBlockingQueue<>());
        } else {
            log.info("Thread pool already started, doing nothing.");
        }
    }

    /**
     * Submit a task to the pool, returning a Future immediately.
     * Also tries to grow the pool, if necessary.
     * @param task
     * @return
     */
    public <T> Future<T> submit(Callable<T> task) {
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

    public void setPoolSize(int newSize) {
        if(newSize > executor.getCorePoolSize()) {
            executor.setMaximumPoolSize(newSize);
            executor.setCorePoolSize(newSize);
        } else {
            executor.setCorePoolSize(newSize);
            executor.setMaximumPoolSize(newSize);
        }
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
        if(!executor.isShutdown()) {
            log.info("Shutting down thread pool");
            executor.shutdown();
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch(InterruptedException e) {
                log.error("Executor was interrupted while awaiting termination: " + e);
            }
        }
    }

}
