package com.codefork.refine.resources;

import java.util.List;
import java.util.Map;

public class StatsDataSource {

    private String name;
    private int numIntervalsStored;
    private List<Map<String, Object>> stats;

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

    public List<Map<String, Object>> getStats() {
        return stats;
    }

    public void setStats(List<Map<String, Object>> stats) {
        this.stats = stats;
    }

}
