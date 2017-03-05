package com.codefork.refine;

public class CachedValue<V> {
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
