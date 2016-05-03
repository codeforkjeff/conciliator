package com.codefork.refine.viaf;

import com.codefork.refine.Cache;
import com.codefork.refine.CacheExpire;
import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.StringUtil;
import com.codefork.refine.resources.Result;
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
import java.util.ArrayList;
import java.util.List;

/**
 * This is the main API for doing VIAF searches.
 */
@Service
public class VIAF {

    public static final boolean DEFAULT_CACHE_ENABLED = true;

    Log log = LogFactory.getLog(VIAF.class);
    private final VIAFService viafService;
    private boolean cacheEnabled = DEFAULT_CACHE_ENABLED;
    private Cache<String, List<Result>> cache = new Cache<String, List<Result>>();
    private final Object cacheLock = new Object();
    private CacheExpire cacheExpire;

    @Autowired
    public VIAF(VIAFService viafService, Config config) {
        this.viafService = viafService;

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
            if(cacheExpire == null) {
                cacheExpire = new CacheExpire(this);

                Thread thread = new Thread(cacheExpire);
                thread.setDaemon(true);
                thread.start();
            }
        } else {
            if(cacheExpire != null) {
                cacheExpire.stopGracefully();
                // null out reference to break circular reference
                // so it can be gc'd
                cacheExpire = null;
            }
        }
    }

    public void setCacheMaxSize(int maxSize) {
        this.cache.setMaxSize(maxSize);
    }

    public int getCacheMaxSize() {
        return this.cache.getMaxSize();
    }
    public void setCacheLifetime(int lifetime) {
        this.cache.setLifetime(lifetime);
    }

    public int getCacheLifetime() {
        return this.cache.getLifetime();
    }

    public void expireCache() {
        if (cacheEnabled && cache.getCount() > 0) {
            // make a copy of the cache, expire entries, then replace
            // original cache with new one
            Cache<String, List<Result>> newCache = new Cache<String, List<Result>>(cache);
            newCache.expireCache();
            synchronized (cacheLock) {
                cache = newCache;
            }
        }
    }

    /**
     * Performs a search.
     * @param query search to perform
     * @return list of search results (a 0-size list if none, or if errors occurred)
     */
    public List<Result> search(SearchQuery query) {
        // TODO: doSearch should differentiate between an error occurring
        // when running a query, and a query returning 0 results.
        // We should only cache successful queries.

        if (cacheEnabled) {
            Cache<String, List<Result>> cacheRef = null;

            // synchronize when getting a reference to the cache;
            // this way, if cache is expired during this block,
            // (i.e. a new Cache instance replaces it), we're still using
            // the "old" cache until this method finishes.
            synchronized (cacheLock) {
                cacheRef = cache;
            }

            String key = query.getHashKey();
            if (!cacheRef.containsKey(key)) {
                cacheRef.put(key, doSearch(query));
            } else {
                log.debug("Cache hit for: " + key);
            }
            return cacheRef.get(key);
        }
        return doSearch(query);
    }

    private List<Result> doSearch(SearchQuery query) {
        List<Result> results = new ArrayList<Result>();
        InputStream response = viafService.doSearch(query.createCqlQueryString(), query.getLimit());

        if(response != null) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            try {
                SAXParser parser = spf.newSAXParser();
                VIAFParser viafParser = new VIAFParser();

                long start = System.currentTimeMillis();
                parser.parse(response, viafParser);
                long parseTime = System.currentTimeMillis() - start;

                try {
                    response.close();
                } catch(IOException ioe) {
                    log.error("Ignoring error from trying to close connection input stream: " + ioe);
                }

                for (VIAFResult viafResult : viafParser.getResults()) {
                    /*
                    log.debug("Result=" + viafResult.getViafId());
                    log.debug("NameType=" + viafResult.getNameType().getViafCode());
                    for(NameEntry nameEntry : viafResult.getNameEntries()) {
                        log.debug("Name=" + nameEntry.getName());
                        log.debug("Sources=" + StringUtils.collectionToDelimitedString(nameEntry.getSources(), ","));
                    }
                    */

                    // if no explicit source was specified, we should use any exact
                    // match if present, otherwise the most common one
                    String name = query.getSource() != null ?
                            viafResult.getNameBySource(query.getSource()) :
                            viafResult.getExactNameOrMostCommonName(query.getQuery());
                    boolean exactMatch = name != null ? name.equals(query.getQuery()) : false;

                    results.add(new Result(
                            viafResult.getViafId(),
                            name,
                            viafResult.getNameType(),
                            StringUtil.levenshteinDistanceRatio(name, query.getQuery()),
                            exactMatch));
                }
                log.debug(String.format("Query: %s - parsing took %dms, got %d results",
                        query.getQuery(), parseTime, viafParser.getResults().size()));
            } catch (ParserConfigurationException ex) {
                log.error("error creating parser: " + ex);
            } catch (SAXException ex) {
                log.error("sax error: " + ex);
            } catch (IOException ex) {
                log.error("ioerror parsing: " + ex);
            }
        }

        return results;
    }

}
