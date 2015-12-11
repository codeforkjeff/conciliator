
package com.codefork.refine;

import com.codefork.refine.resources.Result;
import com.codefork.refine.viaf.VIAF;

import java.util.ArrayList;
import java.util.List;

public class SearchThread implements Runnable {
    private final VIAF viaf;
    private final SearchQuery searchQuery;
    // default to an empty list in case there's an exception in run()
    private List<Result> results = new ArrayList<Result>();

    public SearchThread(VIAF viaf, SearchQuery searchQuery) {
        this.viaf = viaf;
        this.searchQuery = searchQuery;
    }

    @Override
    public void run() {
        results = viaf.search(searchQuery);
    }

    public List<Result> getResults() {
        return results;
    }

}
