package com.codefork.refine.orcid;

import com.codefork.refine.PropertyValue;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Orcid extends WebServiceDataSource {
    public static String EXTRA_PARAM_MODE_FROM_PATH = "mode";
    public static String MODE_SMART_NAMES = "smartnames";

    Log log = LogFactory.getLog(Orcid.class);

    private SAXParserFactory spf = SAXParserFactory.newInstance();

    private static String createQueryString(SearchQuery query) {
        StringBuffer buf = new StringBuffer();
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
    private static String createSearchFieldsQueryString(SearchQuery query) {
        StringBuffer buf = new StringBuffer();
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

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(Map<String, String> extraParams) {
        if(MODE_SMART_NAMES.equals(extraParams.get(EXTRA_PARAM_MODE_FROM_PATH))) {
            return new OrcidMetaDataResponse(getName() + " - Smart Names Mode");
        }
        return new OrcidMetaDataResponse(getName());
    }

    @Override
    public Map<String, String> parseRequestToExtraParams(HttpServletRequest request) {
        String[] parts = request.getServletPath().split("/");
        String dataSourceStr = parts[2];
        String mode = null;
        if (parts.length > 3) {
            mode = parts[3];
        }

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(EXTRA_PARAM_MODE_FROM_PATH, mode);
        return extraParams;
    }

    /**
     * Parse name into given name, family name parts. returns null if name is too complicated to be
     * parsed.
     * @param name
     * @return
     */
    public static String[] parseName(String name) {
        int numCommas = name.length() - name.replace(",", "").length();
        if(numCommas == 1) {
            String[] namePieces = name.split(",");
            if(namePieces.length == 2) {
                return new String[]{
                        namePieces[1].trim(),
                        namePieces[0].trim()
                };
            }
        } else {
            String[] namePieces = name.split("\\s+");
            if(namePieces.length == 2) {
                return namePieces;
            }
        }
        return null;
    }

    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        List<Result> results = Collections.emptyList();

        if(MODE_SMART_NAMES.equals(query.getExtraParams().get(EXTRA_PARAM_MODE_FROM_PATH))) {
            String name = query.getQuery();
            String[] namePieces = parseName(name);
            if(namePieces != null) {
                results = searchSmartNames(query, namePieces[0], namePieces[1]);
            }
        }
        if(results.isEmpty()) {
            results = searchKeyword(query);
        }
        return results;
    }

    private List<Result> searchSmartNames(SearchQuery query, String givenName, String familyName) throws Exception {
        String q = String.format("given-names:%s AND family-name:%s", givenName, familyName);
        String fields = createSearchFieldsQueryString(query);
        if(fields.length() > 0) {
            q += " " + fields;
        }
        String url = String.format("http://pub.orcid.org/v1.2/search/orcid-bio/?rows=%d&q=", query.getLimit()) +
                UriUtils.encodeQueryParam(q, "UTF-8");
        return doSearch(query, url);
    }

    private List<Result> searchKeyword(SearchQuery query) throws Exception {
        String q = createQueryString(query);
        String url = String.format("http://pub.orcid.org/v1.2/search/orcid-bio/?rows=%d&q=", query.getLimit()) +
                UriUtils.encodeQueryParam(q, "UTF-8");
        return doSearch(query, url);
    }

    private List<Result> doSearch(SearchQuery query, String url) throws Exception {
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
