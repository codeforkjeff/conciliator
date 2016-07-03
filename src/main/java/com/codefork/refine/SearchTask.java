
package com.codefork.refine;

import com.codefork.refine.resources.Result;
import com.codefork.refine.viaf.VIAF;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.Callable;

public class SearchTask implements Callable<SearchResult> {
    private final VIAF viaf;
    private final SearchQuery searchQuery;

    private final String key;
    private Log log = LogFactory.getLog(SearchTask.class);

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
            log.error(String.format("error for query=%s: %s", searchQuery.getQuery(), e));
            if (e.toString().contains("HTTP response code: 429")) {
                return new SearchResult(key, SearchResult.ErrorType.TOO_MANY_REQUESTS);
            }
            return new SearchResult(key, SearchResult.ErrorType.UNKNOWN);
        }
        return new SearchResult(key, results);
    }

}
