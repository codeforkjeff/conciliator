package com.codefork.refine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CacheTest {

    @Test
    public void testBasic() throws Exception {
        Cache<String, String> cache = new Cache<String, String>("test cache");
        cache.put("hi", "testing");
        assertTrue(cache.containsKey("hi"));
        assertEquals("testing", cache.get("hi"));
        assertEquals(1, cache.getCount());
    }

    @Test
    public void testExpire() throws Exception {
        Cache<String, String> cache = new Cache<String, String>("test cache");
        cache.put("hi", "testing");
        assertTrue(cache.containsKey("hi"));
        assertEquals("testing", cache.get("hi"));
        assertEquals(1, cache.getCount());

        cache.setLifetime(1);
        assertEquals(1, cache.getLifetime());
        Thread.sleep(1500);
        Cache<String, String> newCache = cache.expireCache();

        assertFalse(newCache.containsKey("hi"));
        assertNull(newCache.get("hi"));
        assertEquals(0, newCache.getCount());
    }

    @Test
    public void testExpireLargeCache() throws Exception {
        Cache<String, String> cache = new Cache<String, String>("test cache");
        cache.setMaxSize(4000);
        cache.setLifetime(3);

        // create some test data
        List<String> ids = new ArrayList<String>();
        for(int i = 0; i < 10000; i++) {
            ids.add(UUID.randomUUID().toString());
        }

        // populate the Cache in 2 phases, with a pause
        for(int i = 0; i < 5000; i++) {
            String id = ids.get(i);
            cache.put(id, id);
        }
        Thread.sleep(3500);
        for(int i = 5000; i < 10000; i++) {
            String id = ids.get(i);
            cache.put(id, id);
        }

        assertEquals(10000, cache.getCount());

        Cache<String, String> newCache = cache.expireCache();

        assertEquals(4000, newCache.getCount());

        for(int i = 6000; i < 10000; i++) {
            assertTrue(newCache.containsKey(ids.get(i)));
        }
    }

    @Test
    public void testOverage() throws Exception {
        Cache<String, String> cache = new Cache<String, String>("test cache");
        cache.put("hi1", "testing1");
        Thread.sleep(100);
        cache.put("hi2", "testing2");
        Thread.sleep(100);
        cache.put("hi3", "testing3");
        Thread.sleep(100);
        cache.put("hi4", "testing4");
        Thread.sleep(100);
        cache.put("hi5", "testing5");
        assertEquals(5, cache.getCount());

        cache.setMaxSize(3);
        assertEquals(3, cache.getMaxSize());

        Cache<String, String> newCache = cache.expireCache();
        assertEquals(3, newCache.getCount());

        assertTrue(newCache.containsKey("hi3"));
        assertTrue(newCache.containsKey("hi4"));
        assertTrue(newCache.containsKey("hi5"));
    }

}
