package com.codefork.refine.openlibrary;

import com.codefork.refine.Config;
import com.codefork.refine.PropertyValue;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.StringUtil;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.datasource.stats.Stats;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component("openlibrary")
public class OpenLibrary extends WebServiceDataSource {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final NameType bookType = new NameType("/book/book", "Book");

    @Autowired
    public OpenLibrary(Config config, CacheManager cacheManager, ThreadPoolFactory threadPoolFactory, ConnectionFactory connectionFactory, Stats stats) {
        super(config, cacheManager, threadPoolFactory, connectionFactory, stats);

        // openlibrary seems to enforce non-simultaneous queries
        getThreadPool().setPoolSize(1);
    }

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(String baseUrl) {
        return new OpenLibraryMetaDataResponse(getName());
    }

    public String createQuery(SearchQuery query, boolean includeProperties) {
        List<String> propValues = new ArrayList<>();
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
        List<Result> results = new ArrayList<>();
        int tries = 0;

        // try twice, first with properties; if no results, try just the main search value
        while(results.size() == 0 && tries < 2) {
            String q = createQuery(query, tries == 0);

            String url = "https://openlibrary.org/search.json?q=" + UriUtils.encodeQueryParam(q, "UTF-8");
            getLog().debug("Making request to " + url);

            HttpURLConnection conn = getConnectionFactory().createConnection(url);

            JsonNode root = mapper.readTree(conn.getInputStream());
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
