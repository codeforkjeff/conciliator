package com.codefork.refine.viaf;

import com.codefork.refine.Cache;
import com.codefork.refine.CacheManager;
import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.Result;
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
     * Performs a search.
     * @param query search to perform
     * @return list of search results (a 0-size list if none, or if errors occurred)
     */
    public List<Result> search(SearchQuery query) {
        if (cacheEnabled) {
            Cache<String, List<Result>> cacheRef = cacheManager.getCache();

            String key = query.getHashKey();
            if (!cacheRef.containsKey(key)) {
                List<Result> results = null;
                // only cache if search was successful
                try {
                    results = doSearch(query);
                    cacheRef.put(key, results);
                } catch(Exception e) {
                    log.error(String.format("error for query=%s: %s", query.getQuery(), e));
                }
                if(results != null) {
                    return results;
                } else {
                    return new ArrayList<Result>();
                }
            } else {
                log.debug("Cache hit for: " + key);
                return cacheRef.get(key);
            }
        }

        try {
            return doSearch(query);
        } catch(Exception e) {
            log.error(String.format("error for query=%s: %s", query.getQuery(), e));
        }
        return new ArrayList<Result>();
    }

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
     * Shuts down the cache thread immediately.
     */
    public void shutdown() {
        if(isCacheEnabled()) {
            cacheManager.stopExpireThread();
        }
    }

}
