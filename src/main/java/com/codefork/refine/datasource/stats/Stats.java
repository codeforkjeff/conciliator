package com.codefork.refine.datasource.stats;

import com.codefork.refine.resources.StatsDataSource;
import com.codefork.refine.resources.StatsReport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Collects approximate statistics.
 * Accuracy is determined by intervalSize: higher is less accurate but also uses less memory and is faster.
 * intervalSize of 5 minutes means stats are accurate within +/- 5 minutes. The smallest reporting bucket
 * should be larger than intervalSize for it to be meaningful.
 * 
 * State of this object gets updated on-demand, when new statistics are reported by getCurrentInterval()
 * or generateReport(). These methods may take a bit of time to run after a long period of inactivity
 * but it saves us from having to do that work in a separate thread.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Stats {
    protected Log log = LogFactory.getLog(this.getClass());

    private String dataSourceName;

    private final LinkedList<Interval> intervals = new LinkedList<>();

    // collect stats in Intervals of this size, in seconds
    private int intervalSize = 60;

    private final List<Bucket> buckets = new ArrayList<>();

    public Stats() {
        // must be added from smallest to largest
        buckets.add(new Bucket("Last 5 mins", 5 * 60));
        buckets.add(new Bucket("Last hour", 60 * 60));
        buckets.add(new Bucket("Last day", 24 * 60 * 60));
        buckets.add(new Bucket("Last week", 7 * 24 * 60 * 60));

        log.debug(String.format("Initialized Stats, max number of intervals stored should be %s",
                buckets.get(buckets.size()-1).getSize() / intervalSize));
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public int getIntervalSize() {
        return intervalSize;
    }

    public void setIntervalSize(int intervalSize) {
        this.intervalSize = intervalSize;
    }

    /**
     * gets the 'current interval': this is the last Interval in the linked list
     * if it's still 'current', otherwise a new Interval is created and automatically appended
     * @return
     */
    public synchronized Interval getCurrentInterval() {
        Interval interval = getLastInterval();
        if(interval == null || interval.isOver()) {
            return appendNewInterval();
        }
        return interval;
    }

    /**
     * Create a new Interval and append it to our list, and trim our intervals
     * @return
     */
    private synchronized Interval appendNewInterval() {
        Interval newInterval = new Interval(Interval.timestamp(), getIntervalSize());
        intervals.add(newInterval);

        trimIntervals();

        return newInterval;
    }

    /**
     * wrapper around getFirst() to return null instead of raising expensive NoSuchElementException
     */
    private synchronized Interval getFirstInterval() {
        return intervals.size() > 0 ? intervals.getFirst() : null;
    }

    /**
     * wrapper around getLast() to return null instead of raising expensive NoSuchElementException
     */
    private synchronized Interval getLastInterval() {
        return intervals.size() > 0 ? intervals.getLast() : null;
    }

    /**
     * Trim off the intervals that exceed our largest bucket size
     */
    private synchronized void trimIntervals() {
        long start = System.currentTimeMillis();
        long intervalsSizeStart = intervals.size();
        Interval interval = getFirstInterval();
        if (interval != null) {
            Bucket largestBucket = buckets.get(buckets.size() - 1);
            long now = Interval.timestamp();
            long threshold = largestBucket.getSize();
            while (now - interval.getStart() > threshold && interval != null) {
                intervals.removeFirst();
                interval = getFirstInterval();
            }
        }
        log.debug(String.format("Removed %s intervals from stats history, took %s ms",
                intervalsSizeStart - intervals.size(), System.currentTimeMillis() - start));
    }

    private synchronized Map<Bucket, Interval> tally(long now) {
        // create a map of intervals to new buckets
        Map<Bucket, Interval> tallyMap = buckets.stream().collect(Collectors.toMap(
                Function.identity(),
                bucket -> new Interval(now - bucket.getSize(), bucket.getSize())
        ));

        List<Interval> tallyIntervals = tallyMap.values().stream().collect(Collectors.toList());

        intervals.forEach(interval -> {
            tallyIntervals.forEach(tallyInterval -> {
                if(interval.getStart() >= tallyInterval.getStart()) {
                    tallyInterval.add(CounterType.QUERIES, interval.get(CounterType.QUERIES));
                    tallyInterval.add(CounterType.ERRORS, interval.get(CounterType.ERRORS));
                }
            });
        });

        return tallyMap;
    }

    public StatsDataSource generateReport() {
        // must force stats to update
        getCurrentInterval();

        long now = Interval.timestamp();

        Map<Bucket, Interval> tallyMap = tally(now);

        Map<String, Map<String, Integer>> stats = tallyMap.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().getLabel(),
                e -> {
                    Map<String, Integer> map = new HashMap<>();
                    map.put(CounterType.QUERIES.name(), e.getValue().get(CounterType.QUERIES));
                    map.put(CounterType.ERRORS.name(), e.getValue().get(CounterType.ERRORS));
                    return map;
                }
        ));

        StatsDataSource statsDataSource = new StatsDataSource();
        statsDataSource.setName(getDataSourceName());
        statsDataSource.setNumIntervalsStored(intervals.size());
        statsDataSource.setStats(stats);

        return statsDataSource;
    }

}
