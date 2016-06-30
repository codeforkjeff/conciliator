package com.codefork.refine;

import com.codefork.refine.resources.Result;

import java.util.List;

/**
 */
public class SearchResult {

    private String key;
    private List<Result> results;

    public SearchResult(String key, List<Result> results) {
        this.key = key;
        this.results = results;
    }

    public String getKey() {
        return key;
    }

    public List<Result> getResults() {
        return results;
    }
}
