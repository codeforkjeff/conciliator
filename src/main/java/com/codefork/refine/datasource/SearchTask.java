
package com.codefork.refine.datasource;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchResult;

import java.util.concurrent.Callable;

public interface SearchTask extends Callable<SearchResult> {

    String getKey();

    SearchQuery getSearchQuery();

    SearchResult call();
}
