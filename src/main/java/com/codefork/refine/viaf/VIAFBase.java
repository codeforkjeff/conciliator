package com.codefork.refine.viaf;

import com.codefork.refine.Cache;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.ThreadPool;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.viaf.sources.NonVIAFSource;
import com.codefork.refine.viaf.sources.Source;
import com.codefork.refine.viaf.sources.VIAFSource;
import org.springframework.web.util.UriUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * VIAF data source base class. This is the "basic" service without
 * source-specific IDs or proxy mode; subclasses provide that functionality.
 *
 * NOTE: VIAF seems to have a limit of 6 simultaneous
 * requests. To be conservative, we default to 4.
 */
public abstract class VIAFBase extends WebServiceDataSource {

    public static final String PROP_CACHE_ENABLED = "cache.enabled";
    public static final String PROP_CACHE_LIFETIME = "cache.lifetime";
    public static final String PROP_CACHE_MAXSIZE = "cache.max_size";

    private SAXParserFactory spf;

    private VIAFSource viafSource = null;
    private Map<String, NonVIAFSource> nonViafSources = new HashMap<>();

    @Override
    public void init() {
        super.init();

        spf = SAXParserFactory.newInstance();

        Properties props = getConfigProperties();

        boolean cacheEnabled = Boolean.valueOf(
                props.getProperty(PROP_CACHE_ENABLED, "true"));
        int cacheLifetime = Integer.valueOf(
                props.getProperty(PROP_CACHE_LIFETIME, String.valueOf(Cache.DEFAULT_LIFETIME)));
        int cacheMaxSize = Integer.valueOf(
                props.getProperty(PROP_CACHE_MAXSIZE, String.valueOf(Cache.DEFAULT_MAXSIZE)));

        setCacheLifetime(cacheLifetime);
        setCacheMaxSize(cacheMaxSize);
        setCacheEnabled(cacheEnabled);
    }

    /**
     * All VIAF instances share a single threadpool
     * @return
     */
    @Override
    protected ThreadPool createThreadPool() {
        return getThreadPoolFactory().getSharedThreadPool("viaf");
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
        String source = query.getViafSource();
        boolean isProxyMode = query.isViafProxyMode();
        if(!isProxyMode) {
            if(viafSource == null) {
                viafSource = new VIAFSource();
            }
            return viafSource;
        }
        return findNonViafSource(source);
    }

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(String baseUrl) {
        return new VIAFMetaDataResponse("VIAF", null, baseUrl);
    }

    /**
     * @return String used for the cql 'query' URL param passed to VIAF;
     *  if nameType is present and invalid, returns null
     */
    public static String createCqlQueryString(SearchQuery searchQuery) {

        String cqlTemplate = "local.mainHeadingEl all \"%s\"";
        if(searchQuery.getNameType() != null) {
            VIAFNameType viafNameType = VIAFNameType.getById(searchQuery.getNameType().getId());
            if(viafNameType == null) {
                return null;
            }
            cqlTemplate = viafNameType.getCqlString();
        }
        String cql = String.format(cqlTemplate, searchQuery.getQuery());

        // NOTE: this query means return all the name records that
        // have an entry for this source; it does NOT mean search the name
        // values for this source ONLY. I think.
        String source = searchQuery.getViafSource();
        if(source != null) {
            cql += String.format(" and local.sources = \"%s\"", source.toLowerCase());
        }

        return cql;
    }

    /**
     * Does actual work of performing a search and parsing the XML.
     * @param query
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        String cql = createCqlQueryString(query);

        if(cql == null) {
            return Collections.emptyList();
        }

        String url = String.format("http://www.viaf.org/viaf/search?query=%s&sortKeys=holdingscount&maximumRecords=%s&httpAccept=application/xml",
                UriUtils.encodeQueryParam(cql, "UTF-8"), query.getLimit());

        HttpURLConnection conn = getConnectionFactory().createConnection(url);
        InputStream response = conn.getInputStream();

        SAXParser parser = spf.newSAXParser();
        VIAFParser viafParser = new VIAFParser(findSource(query), query);

        long start = System.currentTimeMillis();
        parser.parse(response, viafParser);
        long parseTime = System.currentTimeMillis() - start;

        try {
            response.close();
            conn.disconnect();
        } catch(IOException ioe) {
            getLog().error("Ignoring error from trying to close input stream and connection: " + ioe);
        }

        List<Result> results = viafParser.getResults();
        getLog().debug(String.format("Query: %s - parsing took %dms, got %d results",
                query.getQuery(), parseTime, results.size()));

        return results;
    }

}
