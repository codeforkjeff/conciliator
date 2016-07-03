package com.codefork.refine.viaf;

import com.codefork.refine.Cache;
import com.codefork.refine.CacheManager;
import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchResult;
import com.codefork.refine.SearchTask;
import com.codefork.refine.ThreadPool;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.viaf.sources.NonVIAFSource;
import com.codefork.refine.viaf.sources.Source;
import com.codefork.refine.viaf.sources.VIAFSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This is the main API for doing VIAF searches.
 */
@Service
public class VIAF {

    public static final boolean DEFAULT_CACHE_ENABLED = true;

    private SAXParserFactory spf;

    Log log = LogFactory.getLog(VIAF.class);
    private final VIAFService viafService;
    private boolean cacheEnabled = DEFAULT_CACHE_ENABLED;
    private CacheManager cacheManager = new CacheManager();

    private ThreadPool threadPool = new ThreadPool();

    private VIAFSource viafSource = null;
    private Map<String, NonVIAFSource> nonViafSources = new HashMap<String, NonVIAFSource>();

    @Autowired
    public VIAF(VIAFService viafService, Config config) {
        this.viafService = viafService;

        spf = SAXParserFactory.newInstance();

        boolean cacheEnabled = Boolean.valueOf(config.getProperties().getProperty("cache.enabled",
                String.valueOf(DEFAULT_CACHE_ENABLED)));
        int cacheLifetime = Integer.valueOf(config.getProperties().getProperty("cache.lifetime",
                String.valueOf(Cache.DEFAULT_LIFETIME)));
        int cacheMaxSize = Integer.valueOf(config.getProperties().getProperty("cache.max_size",
                String.valueOf(Cache.DEFAULT_MAXSIZE)));

        setCacheLifetime(cacheLifetime);
        setCacheMaxSize(cacheMaxSize);
        setCacheEnabled(cacheEnabled);
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

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Factory method for getting a NonVIAFSource object
     */
    public NonVIAFSource findNonViafSource(String code) {
        if(!nonViafSources.containsKey(code)) {
            nonViafSources.put(code, new NonVIAFSource(code));
        }
        return nonViafSources.get(code);
    }

    /**
     * Factory method for getting a Source object
     * @param query
     * @return
     */
    public Source findSource(SearchQuery query) {
        if(!query.isProxyMode()) {
            if(viafSource == null) {
                viafSource = new VIAFSource();
            }
            return viafSource;
        }
        return findNonViafSource(query.getSource());
    }

    /**
     * This is the entry point for running a set of queries.
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
    public Map<String, SearchResponse> search(Map<String, SearchQuery> queryEntries) {
        Map<String, SearchResponse> allResults = new HashMap<String, SearchResponse>();

        Map<String, SearchResult> results = doSearchInThreadPool(queryEntries);

        // adjust thread pool if necessary on unsuccessful results
        for(Map.Entry<String, SearchResult> queryEntry : results.entrySet()) {
            SearchResult searchResult = queryEntry.getValue();
            if(!searchResult.isSuccessful()) {
                if (SearchResult.ErrorType.TOO_MANY_REQUESTS.equals(searchResult.getErrorType())) {
                    threadPool.shrink();
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

        // second tries
        Map<String, SearchResult> resultsFromSecondTries = doSearchInThreadPool(secondTries);

        // merge into results
        results.putAll(resultsFromSecondTries);

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
        return allResults;
    }

    /**
     * This method sends a single set of queries to the threadpool,
     * waits for the futures to complete, and returns results.
     *
     * @param queryEntries
     * @return
     */
    private Map<String, SearchResult> doSearchInThreadPool(Map<String, SearchQuery> queryEntries) {
        Map<String, SearchResult> results = new HashMap<String, SearchResult>();

        List<SearchTask> tasks = new ArrayList<SearchTask>();
        for (Map.Entry<String, SearchQuery> queryEntry : queryEntries.entrySet()) {
            SearchTask task = new SearchTask(this, queryEntry.getKey(), queryEntry.getValue());
            tasks.add(task);
        }

        List<Future<SearchResult>> futures = new ArrayList<Future<SearchResult>>();
        for (SearchTask task : tasks) {
            futures.add(threadPool.submit(task));
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
    public List<Result> search(SearchQuery query) throws ParserConfigurationException, SAXException, IOException {
        if (cacheEnabled) {
            Cache<String, List<Result>> cacheRef = cacheManager.getCache();

            String key = query.getHashKey();
            if (!cacheRef.containsKey(key)) {
                List<Result> results = doSearch(query);
                // only cache if search was successful
                cacheRef.put(key, results);
                return results;
            } else {
                log.debug("Cache hit for: " + key);
                return cacheRef.get(key);
            }
        }

        return doSearch(query);
    }

    /**
     * Does actual work of performing a search and parsing the XML.
     * @param query
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private List<Result> doSearch(SearchQuery query) throws ParserConfigurationException, SAXException, IOException {
        HttpURLConnection conn = viafService.doSearch(query.createCqlQueryString(), query.getLimit());
        InputStream response = conn.getInputStream();

        SAXParser parser = spf.newSAXParser();
        VIAFParser viafParser = new VIAFParser();

        long start = System.currentTimeMillis();
        parser.parse(response, viafParser);
        long parseTime = System.currentTimeMillis() - start;

        try {
            response.close();
            conn.disconnect();
        } catch(IOException ioe) {
            log.error("Ignoring error from trying to close input stream and connection: " + ioe);
        }

        List<Result> results = new ArrayList<Result>();
        for (VIAFResult viafResult : viafParser.getResults()) {
            /*
            log.debug("Result=" + viafResult.getViafId());
            log.debug("NameType=" + viafResult.getNameType().getViafCode());
            for(NameEntry nameEntry : viafResult.getNameEntries()) {
                log.debug("Name=" + nameEntry.getName());
                log.debug("Sources=" + StringUtils.collectionToDelimitedString(nameEntry.getSources(), ","));
            }
            */

            Source source = findSource(query);
            results.add(source.formatResult(query, viafResult));
        }
        log.debug(String.format("Query: %s - parsing took %dms, got %d results",
                query.getQuery(), parseTime, viafParser.getResults().size()));

        return results;
    }

    /**
     * Does cleanup of member objects: shuts down the cache thread
     * and the thread pool.
     */
    public void shutdown() {
        if(isCacheEnabled()) {
            cacheManager.stopExpireThread();
        }
        threadPool.shutdown();
    }

}
