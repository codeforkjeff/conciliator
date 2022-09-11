package com.codefork.refine.solr;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.datasource.stats.Stats;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Data source for a Solr interface
 */
@Component("solr")
public class Solr extends WebServiceDataSource {

    public static final String PROP_URL_DOCUMENT = "url.document";
    public static final String PROP_URL_QUERY = "url.query";
    public static final String PROP_FIELD_ID = "field.id";
    public static final String PROP_FIELD_NAME = "field.name";
    public static final String PROP_FIELD_NAME_MULTIVALUE_STRATEGY = "field.name.multivalue.strategy";
    public static final String PROP_FIELD_NAME_MULTIVALUE_DELIMITER = "field.name.multivalue.delimiter";
    public static final String PROP_NAMETYPE_ID = "nametype.id";
    public static final String PROP_NAMETYPE_NAME = "nametype.name";

    Log log = LogFactory.getLog(Solr.class);

    private SAXParserFactory spf = SAXParserFactory.newInstance();

    @Autowired
    public Solr(Config config, CacheManager cacheManager, ThreadPoolFactory threadPoolFactory, ConnectionFactory connectionFactory, Stats stats) {
        super(config, cacheManager, threadPoolFactory, connectionFactory, stats);
    }

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(String baseUrl) {
        return new SolrMetaDataResponse(getName(), getConfigProperties().getProperty(PROP_URL_DOCUMENT));
    }

    public String createURL(SearchQuery query) throws Exception {
        String urlTemplate = getConfigProperties().getProperty(PROP_URL_QUERY);
        return urlTemplate.replace("{{QUERY}}", UriUtils.encodeQueryParam(query.getQuery(), "UTF-8"))
                .replace("{{ROWS}}", String.valueOf(query.getLimit()));
    }

    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        String url = createURL(query);
        log.debug("Making request to " + url);
        HttpURLConnection conn = getConnectionFactory().createConnection(url);

        InputStream response = conn.getInputStream();

        MultiValueFieldStrategy multiValueFieldStrategy = MultiValueFieldStrategy.CONCAT;
        if(MultiValueFieldStrategy.CONCAT.toString().toLowerCase().equals(getConfigProperties().getProperty(PROP_FIELD_NAME_MULTIVALUE_STRATEGY))) {
            multiValueFieldStrategy = MultiValueFieldStrategy.CONCAT;
        } else if(MultiValueFieldStrategy.FIRST.toString().toLowerCase().equals(getConfigProperties().getProperty(PROP_FIELD_NAME_MULTIVALUE_STRATEGY))) {
            multiValueFieldStrategy = MultiValueFieldStrategy.FIRST;
        }

        SAXParser parser = spf.newSAXParser();
        SolrParser solrParser = new SolrParser(
                getConfigProperties().getProperty(PROP_FIELD_ID),
                getConfigProperties().getProperty(PROP_FIELD_NAME),
                multiValueFieldStrategy,
                getConfigProperties().getProperty(PROP_FIELD_NAME_MULTIVALUE_DELIMITER, ", "),
                new NameType(getConfigProperties().getProperty(PROP_NAMETYPE_ID),
                        getConfigProperties().getProperty(PROP_NAMETYPE_NAME)));

        long start = System.currentTimeMillis();
        parser.parse(response, solrParser);
        long parseTime = System.currentTimeMillis() - start;

        try {
            response.close();
            conn.disconnect();
        } catch(IOException ioe) {
            log.error("Ignoring error from trying to close input stream and connection: " + ioe);
        }

        log.debug(String.format("Query: %s - parsing took %dms, got %d results",
                query.getQuery(), parseTime, solrParser.getResults().size()));

        return solrParser.getResults();
    }
}
