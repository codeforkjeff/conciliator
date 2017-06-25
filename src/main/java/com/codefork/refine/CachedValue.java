package com.codefork.refine;

public class CachedValue<K, V> {
    private K key;
    private V value;
    private long timestamp;

    public CachedValue(K key, V value, long timestamp) {
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
