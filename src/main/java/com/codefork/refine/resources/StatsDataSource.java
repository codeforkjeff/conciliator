package com.codefork.refine.resources;

import java.util.Map;

public class StatsDataSource {

    private String name;
    private int numIntervalsStored;
    private Map<String, Map<String, Integer>> stats;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumIntervalsStored() {
        return numIntervalsStored;
    }

    public void setNumIntervalsStored(int numIntervalsStored) {
        this.numIntervalsStored = numIntervalsStored;
    }

    public Map<String, Map<String, Integer>> getStats() {
        return stats;
    }

    public void setStats(Map<String, Map<String, Integer>> stats) {
        this.stats = stats;
    }

}
