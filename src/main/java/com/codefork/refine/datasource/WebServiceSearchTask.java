package com.codefork.refine.datasource;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchResult;
import com.codefork.refine.resources.Result;

import java.util.Comparator;
import java.util.List;

/**
 * A search task that contains a reference to a WebServiceDataSource
 * so that we can call its searchCheckCache(), and hence search(),
 * methods.
 */
public class WebServiceSearchTask implements SearchTask {

    static Comparator<Result> BY_SCORE_REVERSED = Comparator.comparingDouble((Result r) -> r.getScore()).reversed();

    private WebServiceDataSource dataSource;
    private String key;
    private SearchQuery searchQuery;

    public WebServiceSearchTask(WebServiceDataSource dataSource, String key, SearchQuery searchQuery) {
        this.key = key;
        this.searchQuery = searchQuery;
        this.dataSource = dataSource;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public SearchQuery getSearchQuery() {
        return searchQuery;
    }

    @Override
    public SearchResult call() {
        List<Result> results;
        String key = getKey();
        SearchQuery searchQuery = getSearchQuery();
        try {
            results = dataSource.searchCheckCache(searchQuery);
        } catch (Exception e) {
            dataSource.getLog().error(String.format("error for query=%s", searchQuery.getQuery()), e);
            if (e.toString().contains("HTTP response code: 429")) {
                return new SearchResult(key, SearchResult.ErrorType.TOO_MANY_REQUESTS);
            }
            return new SearchResult(key, SearchResult.ErrorType.UNKNOWN);
        }
        results.sort(BY_SCORE_REVERSED);
        return new SearchResult(key, results);
    }

}
