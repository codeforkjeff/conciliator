package com.codefork.refine.viaf;

import com.codefork.refine.ThreadPool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadPoolTest {

    @Test
    public void testShrinkAndGrow() throws Exception {
        // test shrink and grow, using much smaller wait period times

        long waitPeriodBeforeShrinkingMs = 1000;
        long waitPeriodBeforeGrowingMs = 2000;
        long waitPeriodBeforeResetMs = 3600000; // 1 hour

        ThreadPool pool = new ThreadPool(waitPeriodBeforeShrinkingMs, waitPeriodBeforeGrowingMs, waitPeriodBeforeResetMs);
        assertEquals(pool.getPoolSize(), ThreadPool.INITIAL_POOL_SIZE);

        pool.shrink();
        assertEquals(pool.getPoolSize(), ThreadPool.INITIAL_POOL_SIZE - 1);

        // this one shouldn't do anything
        pool.shrink();
        assertEquals(pool.getPoolSize(), ThreadPool.INITIAL_POOL_SIZE - 1);

        // wait a bit, then shrink
        Thread.sleep(waitPeriodBeforeShrinkingMs + 500);
        pool.shrink();
        assertEquals(pool.getPoolSize(), ThreadPool.INITIAL_POOL_SIZE - 2);

        // this one shouldn't do anything
        pool.grow();
        assertEquals(pool.getPoolSize(), ThreadPool.INITIAL_POOL_SIZE - 2);

        // wait a bit, then grow
        Thread.sleep(waitPeriodBeforeGrowingMs + 500);
        pool.grow();
        assertEquals(pool.getPoolSize(), ThreadPool.INITIAL_POOL_SIZE - 1);

        pool.shutdown();
    }
}
