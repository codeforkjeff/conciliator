package com.codefork.refine.datasource;

import com.codefork.refine.Application;
import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchResult;
import com.codefork.refine.StringUtil;
import com.codefork.refine.ThreadPool;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.stats.CounterType;
import com.codefork.refine.datasource.stats.Interval;
import com.codefork.refine.datasource.stats.Stats;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

/**
 * A data source that queries a web service API using a threadpool
 * for connections, and caching the results.
 */
public abstract class WebServiceDataSource extends DataSource {

    public static final boolean DEFAULT_CACHE_ENABLED = false;

    private boolean cacheEnabled = DEFAULT_CACHE_ENABLED;

    private CacheManager cacheManager;

    private ThreadPoolFactory threadPoolFactory;

    private ThreadPool threadPool;

    private ConnectionFactory connectionFactory;

    public WebServiceDataSource(
            Config config,
            CacheManager cacheManager,
            ThreadPoolFactory threadPoolFactory,
            ConnectionFactory connectionFactory,
            Stats stats) {
        super(config, stats);
        this.cacheManager = cacheManager;
        this.threadPoolFactory = threadPoolFactory;
        this.connectionFactory = connectionFactory;

        this.threadPool = createThreadPool();

        Properties props = getConfig().getProperties();
        if(props.containsKey(Config.PROP_CACHE_ENABLED)) {
            setCacheEnabled(Boolean.valueOf(props.getProperty(Config.PROP_CACHE_ENABLED)));
        }
   }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * this triggers starting/stopping of thread of expire cache
     * @param cacheEnabled
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    protected ThreadPoolFactory getThreadPoolFactory() {
        return threadPoolFactory;
    }

    /**
     * In cases where a service has multiple WebServiceDataSource instances (like Orcid)
     * this method should probably be overridden to share the same pool.
     */
    protected ThreadPool createThreadPool() {
        return threadPoolFactory.createThreadPool();
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }


    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Does cleanup of member objects: shuts down the cache thread
     * and the thread pool.
     */
    public void shutdown() {
        super.shutdown();
        getThreadPoolFactory().releaseThreadPool(getThreadPool());
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

        Map<String, SearchResponse> allResults = new HashMap<>();

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
        Map<String, SearchQuery> secondTries = new HashMap<>();
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
                allResults.put(indexKey, new SearchResponse(new ArrayList<>()));
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
        Map<String, SearchResult> results = new HashMap<>();

        ExecutorCompletionService<SearchResult> completionService = getThreadPool().createCompletionService(SearchResult.class);

        queryEntries.entrySet().stream().forEach(queryEntry -> {
            SearchTask task = createSearchTask(queryEntry.getKey(), queryEntry.getValue());
            completionService.submit(task);
        });

        for(int i = 0; i < queryEntries.size(); i++) {
            try {
                SearchResult result = completionService.take().get();
                String indexKey = result.getKey();
                results.put(indexKey, result);
            } catch (InterruptedException | ExecutionException e) {
                log.error("searchUsingThreadPool: error getting value from future: " + StringUtil.getStackTrace(e));
            }
        }

        try {
            long start = System.currentTimeMillis();
            updateStats(results.values());
            log.debug(String.format("updateStats took %s ms", System.currentTimeMillis() - start));
        } catch(Exception e) {
            log.error("error in updateStats(), ignoring and continuing: " + StringUtil.getStackTrace(e));
        }

        return results;
    }

    // TODO: remove throws when this is more solid
    private void updateStats(Collection<SearchResult> results) throws Exception {
        Map<CounterType, Integer> counts = results.stream().collect(
                HashMap::new,
                (partial, element) -> {
                    partial.put(CounterType.QUERIES, partial.getOrDefault(CounterType.QUERIES, 0) + 1);
                    if(!element.isSuccessful()) {
                        partial.put(CounterType.ERRORS, partial.getOrDefault(CounterType.ERRORS, 0) + 1);
                    }
                },
                (e1, e2) -> { e1.putAll(e2); });

        Interval interval = getStats().getCurrentInterval();
        interval.add(CounterType.QUERIES, counts.getOrDefault(CounterType.QUERIES, 0));
        interval.add(CounterType.ERRORS, counts.getOrDefault(CounterType.ERRORS, 0));
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
            Cache cache = getCacheManager().getCache(Application.CACHE_DEFAULT);

            String key = getClass().getSimpleName() + "|" + query.getHashKey();
            Cache.ValueWrapper value = cache.get(key);

            List<Result> results;
            if(value != null) {
                log.info("Cache hit for: " + key);
                results = (List<Result>) value.get();
            } else {
                results = search(query);
                cache.put(key, results);
            }
            return results;
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
