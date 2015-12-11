package com.codefork.refine.resources;

import java.util.List;

/**
 * A search response.
 */
public class SearchResponse {
    
    /**
     * OpenRefine expects 'result' key even though it's a list 
     * and should otherwise be plural.
     */
    private List<Result> result;
    
    public SearchResponse(List<Result> results) {
        this.result = results;
    }

    public List<Result> getResult() {
        return result;
    }

    public void setResult(List<Result> result) {
        this.result = result;
    }
    
}
