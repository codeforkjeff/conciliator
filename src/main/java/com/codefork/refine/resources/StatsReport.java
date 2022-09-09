package com.codefork.refine.resources;

import java.util.List;
import java.util.Map;

public class StatsReport {

    private long timestamp;
    private String date;
    public List<StatsDataSource> dataSources;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<StatsDataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<StatsDataSource> dataSources) {
        this.dataSources = dataSources;
    }
}
