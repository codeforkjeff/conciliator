package com.codefork.refine.orcid;

import com.codefork.refine.PropertyValue;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchQueryFactory;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriUtils;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reconcile/orcid")
public class Orcid extends WebServiceDataSource {

    Log log = LogFactory.getLog(Orcid.class);

    private SAXParserFactory spf = SAXParserFactory.newInstance();

    @RequestMapping(value = "/smartnames")
    @ResponseBody
    public OrcidMetaDataResponse smartNamesServiceMetaData() {
        return new OrcidMetaDataResponse(getName() + " - Smart Names Mode");
    }

    @RequestMapping(value = "/smartnames", params = "query")
    @ResponseBody
    public SearchResponse smartNamesQuerySingle(@RequestParam(value = "query") String query) {
        return querySingle(query, new SmartNamesModeSearchQueryFactory());
    }

    @RequestMapping(value = "/smartnames", params = "queries")
    @ResponseBody
    public Map<String, SearchResponse> smartNamesQueryMultiple(@RequestParam(value = "queries") String queries) {
        return queryMultiple(queries, new SmartNamesModeSearchQueryFactory());
    }

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
    public ServiceMetaDataResponse createServiceMetaDataResponse() {
        return new OrcidMetaDataResponse(getName());
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

        if(query.isOrcidSmartNamesMode()) {
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

    private static class SmartNamesModeSearchQueryFactory implements SearchQueryFactory {

        @Override
        public SearchQuery createSearchQuery(JsonNode queryStruct) {
            SearchQuery searchQuery = new SearchQuery(queryStruct);
            searchQuery.setOrcidSmartNamesMode(true);
            return searchQuery;
        }

        @Override
        public SearchQuery createSearchQuery(String query, int limit, NameType nameType, String typeStrict) {
            SearchQuery searchQuery = new SearchQuery(query, limit, nameType, typeStrict);
            searchQuery.setOrcidSmartNamesMode(true);
            return searchQuery;
        }
    }

}
