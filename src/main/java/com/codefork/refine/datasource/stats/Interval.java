
package com.codefork.refine.datasource.stats;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * An interval is defined by its 'start' unix timestamp and its duration
 */
public class Interval {

    private final Map<CounterType, Integer> counters = new HashMap<>();
    private long start;
    private long duration;

    /**
     * Seconds since epoch
     */
    public static final long timestamp() {
        return Instant.now().getEpochSecond();
    }

    public Interval(long start, long duration) {
        this.start = start;
        this.duration = duration;
    }

    public long getStart() {
        return start;
    }

    public boolean isOver() {
        return timestamp() - getStart() > duration;
    }

    public void add(CounterType counterType, int value) {
        counters.put(counterType, counters.getOrDefault(counterType, 0) + value);
    }

    public int get(CounterType counterType) {
        return counters.getOrDefault(counterType, 0);
    }

}
