
package com.codefork.refine;

import com.codefork.refine.resources.Result;
import com.codefork.refine.viaf.VIAF;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class SearchTask implements Callable<SearchResult> {
    private final VIAF viaf;
    private final SearchQuery searchQuery;

    private final String key;

    public SearchTask(VIAF viaf, String key, SearchQuery searchQuery) {
        this.viaf = viaf;
        this.key = key;
        this.searchQuery = searchQuery;
    }

    @Override
    public SearchResult call() {
        List<Result> results;
        try {
            results = viaf.search(searchQuery);
        } catch(Exception e) {
            results = new ArrayList<Result>();
        }
        return new SearchResult(key, results);
    }

}
