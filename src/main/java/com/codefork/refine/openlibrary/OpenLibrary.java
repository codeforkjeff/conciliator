package com.codefork.refine.openlibrary;

import com.codefork.refine.PropertyValue;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.StringUtil;
import com.codefork.refine.ThreadPool;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.util.UriUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OpenLibrary extends WebServiceDataSource {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final NameType bookType = new NameType("/book/book", "Book");

    @Override
    public ThreadPool createThreadPool() {
        // openlibrary seems to enforce non-simultaneous queries
        return new ThreadPool(1);
    }

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(Map<String, String> extraParams) {
        return new OpenLibraryMetaDataResponse(getName());
    }

    public String createQuery(SearchQuery query, boolean includeProperties) {
        List<String> propValues = new ArrayList<String>();
        for(PropertyValue val : query.getProperties().values()) {
            propValues.add(val.asString());
        }

        String q = query.getQuery();
        if(includeProperties) {
            String propValuesString = StringUtil.join(propValues, " ");
            if(propValuesString.length() > 0) {
                q += " " + propValuesString;
            }
        }
        return q;
    }

    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        List<Result> results = new ArrayList<Result>();
        int tries = 0;

        // try twice, first with properties; if no results, try just the main search value
        while(results.size() == 0 && tries < 2) {
            String q = createQuery(query, tries == 0);

            String url = String.format("https://openlibrary.org/search.json?q=", query.getLimit()) +
                    UriUtils.encodeQueryParam(q, "UTF-8");
            getLog().debug("Making request to " + url);

            JsonNode root = mapper.readTree(new URL(url));
            JsonNode docs = root.get("docs");
            if(docs.isArray()) {
                Iterator<JsonNode> iter = docs.iterator();
                while(iter.hasNext() && results.size() < query.getLimit()) {
                    JsonNode doc = iter.next();
                    String title = doc.get("title").asText();
                    String key = doc.get("key").asText();
                    results.add(new Result(key, title, bookType, 1.0, false));
                }
            }

            tries++;
        }

        return results;
    }

}
