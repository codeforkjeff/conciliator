package com.codefork.refine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple cache implementation. This does NOT do any locking. Clients should
 * lock when getting a reference to a Cache instance, in order to guarantee
 * consistency. To expire the cache, call expireCache() to get a new instance,
 * then replace the original instance.
 */
public class Cache<K, V> {

    public static final int DEFAULT_LIFETIME = 60 * 30; // 30 mins
    public static final int DEFAULT_MAXSIZE = 10000;

    Log log = LogFactory.getLog(Cache.class);

    private int lifetime = DEFAULT_LIFETIME; // in seconds
    private int maxSize = DEFAULT_MAXSIZE;
    private ConcurrentHashMap<K, CachedValue> cacheMap = new ConcurrentHashMap<K, CachedValue>();
    private String name;

    public Cache(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
        put(key, value, System.currentTimeMillis());
    }

    public void put(K key, CachedValue value) {
        cacheMap.put(key, value);
    }
    private void put(K key, V value, long timestamp) {
        cacheMap.put(key, new CachedValue(value, timestamp));
    }

    public V get(K key) {
        if(containsKey(key)) {
            return cacheMap.get(key).getValue();
        }
        return null;
    }

    /**
     * Returns a copy of this instance, removing entries older than this cache's lifetime,
     * and discarding entries if we're over the maxSize limit.
     */
    public Cache<K, V> expireCache() {
        Cache<K, V> newCache = new Cache<K, V>(getName());
        newCache.setLifetime(getLifetime());
        newCache.setMaxSize(getMaxSize());

        long now = System.currentTimeMillis();
        int count = 0;
        int expiredCount = 0;
        int overageCount = 0;

        // processing keys in descending timestamp order makes
        // it easier to break out of the copy loop when we hit maxSize
        final ConcurrentHashMap<K, CachedValue> oldMap = getMap();
        K[] sorted = (K[]) oldMap.keySet().toArray();
        Arrays.sort(sorted, new ReverseTimestampComparator());
        int total = oldMap.size();

        // loop through keys, newest first
        for(K key : sorted) {
            // we don't need to continue if we encounter an expired entry
            // or if we exceed maxSize
            if(now - oldMap.get(key).getTimestamp() >= getLifetime() * 1000) {
                expiredCount = total - count;
                break;
            } else if(count >= maxSize) {
                overageCount = total - maxSize;
                break;
            } else {
                newCache.put(key, oldMap.get(key));
            }
            count++;
        }

        int remaining = total - expiredCount - overageCount;
        if(expiredCount> 0 || overageCount > 0) {
            log.debug(getName() + ": expireCache() took " + (System.currentTimeMillis() - now) + "ms: removed " + expiredCount + " expired entries, " + overageCount + " entries over maxsize limit; " + remaining + " remaining");
        }
        return newCache;
    }

    private ConcurrentHashMap<K, CachedValue> getMap() {
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

    /**
     * Reverse sorts a collection of keys by their cache entry timestamp.
     */
    public class ReverseTimestampComparator implements Comparator<K> {
        @Override
        public int compare(K o1, K o2) {
            long ts1 = getMap().get(o1).getTimestamp();
            long ts2 = getMap().get(o2).getTimestamp();
            // reverse order sort
            if(ts1 < ts2) {
                return 1;
            } else if(ts1 == ts2) {
                return 0;
            }
            return -1;
        }
    }

}
