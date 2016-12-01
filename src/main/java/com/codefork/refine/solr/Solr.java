package com.codefork.refine.solr;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.orcid.OrcidMetaDataResponse;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.util.UriUtils;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Data source for any Solr interface.
 * Config file should define properties as follows:
 *
 * datasource.solr=com.codefork.refine.solr.Solr
 * datasource.solr.name=Arbitrary Solr Instance
 * datasource.solr.url.query=http://localhost:8983/solr/blacklight-core/select?q=QUERY&rows=ROWS&wt=xml
 * datasource.solr.url.document=http://localhost:8983/solr/blacklight-core/get?id=ID
 * datasource.solr.field.name=name
 */
public class Solr extends WebServiceDataSource {

    Log log = LogFactory.getLog(Solr.class);

    private SAXParserFactory spf = SAXParserFactory.newInstance();

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(Map<String, String> extraParams) {
        // TODO
        // use url.document key
        return new OrcidMetaDataResponse(getName());
    }

    public String createURL(SearchQuery query) throws Exception {
        String urlTemplate = getConfigProperties().getProperty("url.query");
        return urlTemplate.replace("QUERY", UriUtils.encodeQueryParam(query.getQuery(), "UTF-8"))
                .replace("ROWS", String.valueOf(query.getLimit()));
    }

    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        String url = createURL(query);
        log.debug("Making request to " + url);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        InputStream response = conn.getInputStream();

        SAXParser parser = spf.newSAXParser();
        SolrResponseParser solrResponseParser = new SolrResponseParser(
                getConfigProperties().getProperty("field.id"),
                getConfigProperties().getProperty("field.name"));

        long start = System.currentTimeMillis();
        parser.parse(response, solrResponseParser);
        long parseTime = System.currentTimeMillis() - start;

        try {
            response.close();
            conn.disconnect();
        } catch(IOException ioe) {
            log.error("Ignoring error from trying to close input stream and connection: " + ioe);
        }

        log.debug(String.format("Query: %s - parsing took %dms, got %d results",
                query.getQuery(), parseTime, solrResponseParser.getResults().size()));

        return solrResponseParser.getResults();
    }
}
