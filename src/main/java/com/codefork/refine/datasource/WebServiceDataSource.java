package com.codefork.refine.datasource;

import com.codefork.refine.Cache;
import com.codefork.refine.CacheManager;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchResult;
import com.codefork.refine.ThreadPool;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A data source that queries a web service API using a threadpool
 * for connections, and caching the results.
 */
public abstract class WebServiceDataSource extends DataSource {

    public static final boolean DEFAULT_CACHE_ENABLED = true;

    Log log = LogFactory.getLog(WebServiceDataSource.class);

    private boolean cacheEnabled = DEFAULT_CACHE_ENABLED;
    private CacheManager cacheManager = new CacheManager(getName() + " Cache");

    private ThreadPool threadPool = new ThreadPool();

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * this triggers starting/stopping of thread of expire cache
     * @param cacheEnabled
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        if(isCacheEnabled()) {
            cacheManager.startExpireThread();
        } else {
            cacheManager.stopExpireThread();
        }
    }

    public void setCacheMaxSize(int maxSize) {
        cacheManager.getCache().setMaxSize(maxSize);
    }

    public int getCacheMaxSize() {
        return cacheManager.getCache().getMaxSize();
    }

    public void setCacheLifetime(int lifetime) {
        cacheManager.getCache().setLifetime(lifetime);
    }

    public int getCacheLifetime() {
        return cacheManager.getCache().getLifetime();
    }

    public void expireCache() {
        cacheManager.expireCache();
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Does cleanup of member objects: shuts down the cache thread
     * and the thread pool.
     */
    public void shutdown() {
        if(isCacheEnabled()) {
            getCacheManager().stopExpireThread();
        }
        getThreadPool().shutdown();
    }

    /**
     * Factory method for creating a search task for given key and SearchQuery.
     * This provides a way to return customized SearchTask subclasses
     * so they have what they need to perform searches.
     * @param key
     * @param searchQuery
     * @return
     */
    public SearchTask createSearchTask(String key, SearchQuery searchQuery) {
        return new WebServiceSearchTask(this, key, searchQuery);
    }

    /**
     * This is the main entry point for running a set of queries.
     * Web app controllers use this.
     *
     * It makes use of the threadpool, shrinking/growing it as necessary
     * in response to HTTP 429 responses from VIAF, and retrying those
     * requests.
     *
     * @param queryEntries map of string ids (e.g. q0, q1, as identified by OpenRefine clients)
     *                     => SearchQuery objects
     * @return map of string ids => SearchResult objects
     */
    @Override
    public Map<String, SearchResponse> search(Map<String, SearchQuery> queryEntries) {
        long start = System.currentTimeMillis();

        Map<String, SearchResponse> allResults = new HashMap<String, SearchResponse>();

        Map<String, SearchResult> results = searchUsingThreadPool(queryEntries);

        // adjust thread pool if necessary on unsuccessful results
        for(Map.Entry<String, SearchResult> queryEntry : results.entrySet()) {
            SearchResult searchResult = queryEntry.getValue();
            if(!searchResult.isSuccessful()) {
                if (SearchResult.ErrorType.TOO_MANY_REQUESTS.equals(searchResult.getErrorType())) {
                    getThreadPool().shrink();
                }
            }
        }

        // figure out which queries need to be done again
        Map<String, SearchQuery> secondTries = new HashMap<String, SearchQuery>();
        for(Map.Entry<String, SearchQuery> queryEntry : queryEntries.entrySet()) {
            String indexKey = queryEntry.getKey();
            if(!results.containsKey(indexKey)) {
                SearchQuery searchQuery = queryEntry.getValue();
                log.info("Submitting second try for query: " + searchQuery.getQuery());
                secondTries.put(indexKey, searchQuery);
            }
        }

        if(secondTries.size() > 0) {
            // sleep a bit, avoid doing second tries immediately
            try {
                Thread.sleep(1500);
            } catch(InterruptedException e) {
                log.error("sleep interrupted before doing second tries");
            }

            // second tries
            Map<String, SearchResult> resultsFromSecondTries = searchUsingThreadPool(secondTries);

            // merge into results
            results.putAll(resultsFromSecondTries);
        }

        // return empty arrays for searches that didn't complete due to errors
        for(Map.Entry<String, SearchResult> queryEntry : results.entrySet()) {
            String indexKey = queryEntry.getKey();
            SearchResult searchResult = queryEntry.getValue();
            if(searchResult.isSuccessful()) {
                allResults.put(indexKey, new SearchResponse(searchResult.getResults()));
            } else {
                allResults.put(indexKey, new SearchResponse(new ArrayList<Result>()));
            }
        }

        log.debug(String.format("%s tasks finished in %s (thread pool size=%s)", queryEntries.size(), System.currentTimeMillis() - start, getThreadPool().getPoolSize()));

        return allResults;
    }

    /**
     * This method sends a single set of queries to the threadpool,
     * waits for the futures to complete, and returns results.
     *
     * @param queryEntries
     * @return
     */
    private Map<String, SearchResult> searchUsingThreadPool(Map<String, SearchQuery> queryEntries) {
        Map<String, SearchResult> results = new HashMap<String, SearchResult>();

        List<SearchTask> tasks = new ArrayList<SearchTask>();
        for (Map.Entry<String, SearchQuery> queryEntry : queryEntries.entrySet()) {
            SearchTask task = createSearchTask(queryEntry.getKey(), queryEntry.getValue());
            tasks.add(task);
        }

        List<Future<SearchResult>> futures = new ArrayList<Future<SearchResult>>();
        for (SearchTask task : tasks) {
            futures.add(getThreadPool().submit(task));
        }

        for (Future<SearchResult> future : futures) {
            try {
                SearchResult result = future.get();
                String indexKey = result.getKey();
                results.put(indexKey, result);
            } catch (InterruptedException e) {
                log.error("error getting value from future: " + e);
            } catch (ExecutionException e) {
                log.error("error getting value from future: " + e);
            }
        }
        return results;
    }

    /**
     * Performs a search for a single query; this entry point checks the cache, if enabled.
     * This is a "lower level" call than search(Map).
     *
     * @param query search to perform
     * @return list of search results (a 0-size list if none, or if errors occurred)
     */
    public List<Result> searchCheckCache(SearchQuery query) throws Exception {
        if (isCacheEnabled()) {
            Cache<String, List<Result>> cacheRef = getCacheManager().getCache();

            String key = query.getHashKey();
            if (!cacheRef.containsKey(key)) {
                List<Result> results = search(query);
                // only cache if search was successful
                cacheRef.put(key, results);
                return results;
            } else {
                log.debug("Cache hit for: " + key);
                return cacheRef.get(key);
            }
        }

        return search(query);
    }

    /**
     * Perform a search. This gets called by other code in this class
     * that takes care of caching and running in a threadpool,
     * so implementations of this method should NOT concern itself with
     * those things.
     * @param query
     * @return
     * @throws Exception
     */
    public abstract List<Result> search(SearchQuery query) throws Exception;

}
