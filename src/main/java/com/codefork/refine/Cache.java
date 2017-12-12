package com.codefork.refine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

    private final Log log = LogFactory.getLog(Cache.class);

    private int lifetime = DEFAULT_LIFETIME; // in seconds
    private int maxSize = DEFAULT_MAXSIZE;
    private final ConcurrentHashMap<K, CachedValue<K, V>> cacheMap;
    // maintain a list of values in the order they were added to the cache.
    // this allows us to expire caches quickly.
    private final List<CachedValue<K, V>> orderedValues;
    private final String name;

    public Cache(String name) {
        this.name = name;
        this.cacheMap = new ConcurrentHashMap<K, CachedValue<K, V>>();
        this.orderedValues = Collections.synchronizedList(new LinkedList<CachedValue<K, V>>());
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

    private void put(K key, CachedValue<K, V> value) {
        cacheMap.put(key, value);
        orderedValues.add(value);
    }

    private void put(K key, V value, long timestamp) {
        put(key, new CachedValue<K, V>(key, value, timestamp));
    }

    public void put(K key, V value) {
        put(key, value, System.currentTimeMillis());
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
        long now = System.currentTimeMillis();

        // strategy here is to selectively copy what we need from cache;
        // this is faster than copying everything and then removing expired and over-max items,
        // since, under the hood, there is no "fast bulk copy" for the data structures we use

        Cache<K, V> newCache = new Cache<K, V>(getName());
        newCache.setLifetime(getLifetime());
        newCache.setMaxSize(getMaxSize());

        int total = cacheMap.size();
        int count = 0;
        int expiredCount = 0;
        int overageCount = 0;

        // loop through keys, newest first
        // use toArray() instead of descendingIterator() b/c latter is not thread safe
        // and will throw ConcurrentModificationExceptions.
        Object[] array = orderedValues.toArray();
        boolean keepGoing = true;
        for(int i = array.length - 1; i >= 0 && keepGoing; i--) {
            @SuppressWarnings (value="unchecked")
            CachedValue<K, V> value = (CachedValue<K, V>) array[i];
            if(now - value.getTimestamp() >= getLifetime() * 1000) {
                expiredCount = total - count;
                keepGoing = false;
            } else if(count >= maxSize) {
                overageCount = total - maxSize;
                keepGoing = false;
            } else {
                newCache.put(value.getKey(), value);
                count++;
           }
        }

        int remaining = total - expiredCount - overageCount;
        if(expiredCount> 0 || overageCount > 0) {
            log.debug(getName() + ": expireCache() took " + (System.currentTimeMillis() - now) + "ms: removed " + expiredCount + " expired entries, " + overageCount + " entries over maxsize limit; " + remaining + " remaining");
        }
        return newCache;
    }

    private ConcurrentHashMap<K, CachedValue<K, V>> getMap() {
        return cacheMap;
    }

}
