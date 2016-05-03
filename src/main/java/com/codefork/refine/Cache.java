package com.codefork.refine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Simple cache implementation. This does NOT do any locking. Clients should
 * lock when getting a reference to a Cache instance, in order to guarantee
 * consistency. To expire the cache, use the copy constructor to first clone
 * the instance, call expireCache() on the new instance, then replace the
 * original instance.
 */
public class Cache<K, V> {

    public static final int DEFAULT_LIFETIME = 60 * 30; // 30 mins
    public static final int DEFAULT_MAXSIZE = 10000;

    Log log = LogFactory.getLog(Cache.class);

    private int lifetime = DEFAULT_LIFETIME; // in seconds
    private int maxSize = DEFAULT_MAXSIZE;
    private HashMap<K, CachedValue> cacheMap = new HashMap<K, CachedValue>();

    public Cache() {
    }

    /**
     * Copy constructor
     */
    public Cache(Cache original) {
        this.cacheMap = (HashMap<K, CachedValue>) original.getMap().clone();
        setLifetime(original.getLifetime());
        setMaxSize(original.getMaxSize());
    }

    public int getLifetime() {
        return lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getCount() {
        return cacheMap.size();
    }

    public boolean containsKey(K key) {
        return cacheMap.containsKey(key);
    }

    public void put(K key, V value) {
        cacheMap.put(key, new CachedValue(value, System.currentTimeMillis()));
    }

    public V get(K key) {
        if(containsKey(key)) {
            return cacheMap.get(key).getValue();
        }
        return null;
    }

    /**
     * Removes entries older than this cache's lifetime,
     * and discard entries if we're over the maxSize limit.
     */
    public void expireCache() {
        long now = System.currentTimeMillis();
        int count = 0;
        int expiredCount = 0;
        int overageCount = 0;
        int total = cacheMap.size();
        Iterator<K> iter = cacheMap.keySet().iterator();
        while(iter.hasNext()) {
            K key = iter.next();
            if(now - cacheMap.get(key).getTimestamp() >= getLifetime() * 1000) {
                iter.remove();
                expiredCount++;
            } else if(count >= maxSize) {
                iter.remove();
                overageCount++;
            }
            count++;
        }
        int remaining = total - expiredCount - overageCount;
        if(expiredCount> 0 || overageCount > 0) {
            log.debug("expireCache() took " + (System.currentTimeMillis() - now) + "ms: removed " + expiredCount + " expired entries, " + overageCount + " entries over maxsize limit; " + remaining + " remaining");
        }
    }

    private HashMap<K, CachedValue> getMap() {
        return cacheMap;
    }

    public class CachedValue {
        private V value;
        private long timestamp;

        public CachedValue(V value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public V getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
