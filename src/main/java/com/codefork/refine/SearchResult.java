package com.codefork.refine;

import com.codefork.refine.resources.Result;

import java.util.List;

/**
 */
public class SearchResult {

    public enum ErrorType {
        UNKNOWN, TOO_MANY_REQUESTS
    }

    private String key;
    private List<Result> results;
    private ErrorType errorType;

    public SearchResult(String key, List<Result> results) {
        this.key = key;
        this.results = results;
    }

    public SearchResult(String key, ErrorType errorType) {
        this.key = key;
        this.errorType = errorType;
    }

    public String getKey() {
        return key;
    }

    public List<Result> getResults() {
        return results;
    }

    public boolean isSuccessful() {
        return errorType == null;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
