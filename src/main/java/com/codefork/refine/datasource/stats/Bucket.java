package com.codefork.refine.datasource.stats;

/**
 * A Bucket represents a length of past time from the present ('size' seconds ago)
 */
public class Bucket {
    private String label;
    private long size;

    public Bucket(String label, long size) {
        this.label = label;
        this.size = size;
    }

    public String getLabel() {
        return label;
    }

    public long getSize() {
        return size;
    }
}
