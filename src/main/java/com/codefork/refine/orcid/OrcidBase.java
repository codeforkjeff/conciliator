package com.codefork.refine.orcid;

import com.codefork.refine.PropertyValue;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.Result;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.util.UriUtils;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * This isn't very "abstract" since it is aware of smartnames mode.
 * But this exists to make it possible to have two subclasses that are
 * @Controllers for different urls.
 */
public abstract class OrcidBase extends WebServiceDataSource {

    Log log = LogFactory.getLog(Orcid.class);

    private SAXParserFactory spf = SAXParserFactory.newInstance();

    protected static String createQueryString(SearchQuery query) {
        StringBuilder buf = new StringBuilder();
        buf.append(query.getQuery());
        String fields = createSearchFieldsQueryString(query);
        if(fields.length() > 0) {
            buf.append(" ");
            buf.append(fields);
        }
        return buf.toString();
    }

    /**
     * creates Solr-style "field:value" query string from properties in SearchQuery
     */
    protected static String createSearchFieldsQueryString(SearchQuery query) {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, PropertyValue> prop : query.getProperties().entrySet()) {
            if(!first) {
                buf.append(" ");
            }
            buf.append(prop.getKey());
            buf.append(":");
            String val = prop.getValue().asString();
            if(val.contains(" ")) {
                buf.append("\"");
                buf.append(val);
                buf.append("\"");
            } else {
                buf.append(val);
            }
            first = false;
        }
        return buf.toString();
    }


    protected List<Result> searchKeyword(SearchQuery query) throws Exception {
        String q = createQueryString(query);
        String url = String.format("http://pub.orcid.org/v1.2/search/orcid-bio/?rows=%d&q=", query.getLimit()) +
                UriUtils.encodeQueryParam(q, "UTF-8");
        return doSearch(query, url);
    }

    protected List<Result> doSearch(SearchQuery query, String url) throws Exception {
        log.debug("Making request to " + url);
        HttpURLConnection conn = getConnectionFactory().createConnection(url);

        InputStream response = conn.getInputStream();

        SAXParser parser = spf.newSAXParser();
        OrcidParser orcidParser = new OrcidParser();

        long start = System.currentTimeMillis();
        parser.parse(response, orcidParser);
        long parseTime = System.currentTimeMillis() - start;

        try {
            response.close();
            conn.disconnect();
        } catch(IOException ioe) {
            log.error("Ignoring error from trying to close input stream and connection: " + ioe);
        }

        log.debug(String.format("Query: %s - parsing took %dms, got %d results",
                query.getQuery(), parseTime, orcidParser.getResults().size()));

        return orcidParser.getResults();
    }

}
