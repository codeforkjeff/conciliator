package com.codefork.refine.datasource;

import com.codefork.refine.Cache;
import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchQueryFactory;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * A super-generic reconciliation data source.
 */
public abstract class DataSource {

    protected Log log = LogFactory.getLog(this.getClass());

    // human readable name
    private String name = this.getClass().getSimpleName();

    // key to use for config file properties (datasource.{configName})
    private String configName = this.getClass().getSimpleName().toLowerCase();

    @Autowired
    private Config config;

    private final ObjectMapper mapper = new ObjectMapper();

    private SearchQueryFactory defaultSearchQueryFactory = new SearchQueryFactory() {
        @Override
        public SearchQuery createSearchQuery(JsonNode queryStruct) {
            return new SearchQuery(queryStruct);
        }

        @Override
        public SearchQuery createSearchQuery(String query, int limit, NameType nameType, String typeStrict) {
            return new SearchQuery(query, limit, nameType, typeStrict);
        }
    };

    @PostConstruct
    public void init() {
        Properties props = getConfigProperties();

        if(props.containsKey("name")) {
            setName(props.getProperty("name"));
        }
    }

    @PreDestroy
    public void shutdown() {
        // no-op
    }

    public Log getLog() {
        return log;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * @return Properties object containing keys relevant to this
     * datasource, with "datasource.name" prefix stripped away.
     */
    public Properties getConfigProperties() {
        return getConfig().getDataSourceProperties(getConfigName());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return name used in the config properties keys for this datasource
     */
    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    /**
     * This is the main entry point for running a set of queries contained in a HTTP request.
     */
    public abstract Map<String, SearchResponse> search(Map<String, SearchQuery> queryEntries);

    /**
     * Returns the service metadata that OpenRefine uses on its first request
     * to the service.
     */
    protected abstract ServiceMetaDataResponse createServiceMetaDataResponse(String baseUrl);

    public SearchQueryFactory getSearchQueryFactory() {
        return defaultSearchQueryFactory;
    }

    @RequestMapping(value = { "", "/" })
    @ResponseBody
    public ServiceMetaDataResponse serviceMetaData(HttpServletRequest request) {
        return createServiceMetaDataResponse(request.getRequestURL().toString());
    }

    @RequestMapping(value = { "", "/" }, params = "query")
    @ResponseBody
    public SearchResponse querySingle(@RequestParam(value = "query") String query) {
        return querySingle(query, getSearchQueryFactory());
    }

    public SearchResponse querySingle(String query, SearchQueryFactory searchQueryFactory) {
        log.debug("query=" + query);
        try {
            SearchQuery searchQuery;
            if (query.startsWith("{")) {
                JsonNode root = mapper.readTree(query);
                searchQuery = searchQueryFactory.createSearchQuery(root);
            } else {
                searchQuery = searchQueryFactory.createSearchQuery(query, 3, null, "should");
            }

            Map<String, SearchQuery> queriesMap = new HashMap<String, SearchQuery>();
            queriesMap.put("q0", searchQuery);

            Map<String, SearchResponse> resultsMap = search(queriesMap);

            return new SearchResponse(resultsMap.get("q0").getResult());
        } catch (JsonProcessingException jse) {
            log.error("Got an error processing JSON: " + jse.toString());
        } catch (IOException ioe) {
            log.error("Got IO error processing JSON: " + ioe.toString());
        }
        return null;
    }

    @RequestMapping(value = { "", "/" }, params = "queries")
    @ResponseBody
    public Map<String, SearchResponse> queryMultiple(@RequestParam(value = "queries") String queries) {
        return queryMultiple(queries, getSearchQueryFactory());
    }

    public Map<String, SearchResponse> queryMultiple(String queries, SearchQueryFactory searchQueryFactory) {
        log.debug("queries=" + queries);
        try {
            JsonNode root = mapper.readTree(queries);

            Map<String, SearchQuery> queriesMap = new HashMap<String, SearchQuery>();

            for(Iterator<Map.Entry<String, JsonNode>> iter = root.fields(); iter.hasNext(); ) {
                Map.Entry<String, JsonNode> fieldEntry = iter.next();

                String indexKey = fieldEntry.getKey();
                JsonNode queryStruct = fieldEntry.getValue();

                SearchQuery searchQuery = searchQueryFactory.createSearchQuery(queryStruct);
                queriesMap.put(indexKey, searchQuery);
            }

            Map<String, SearchResponse> resultsMap = search(queriesMap);

            log.debug(String.format("response=%s", new DeferredJSON(resultsMap)));

            return resultsMap;
        } catch (JsonProcessingException jse) {
            log.error("Got an error processing JSON: " + jse.toString());
        } catch (IOException ioe) {
            log.error("Got IO error processing JSON: " + ioe.toString());
        }
        return null;
    }

    @RequestMapping(value = { "/suggest" })
    @ResponseBody
    public Object suggest() {
        // TODO
        return "in suggest";
    }

    /**
     * Overrides toString() to provide JSON representation of an object on-demand.
     * This allows us to avoid doing the JSON serialization if the logger
     * doesn't actually print it.
     */
    private class DeferredJSON {

        private final Object o;

        public DeferredJSON(Object o) {
            this.o = o;
        }

        @Override
        public String toString() {
            try {
                return mapper.writeValueAsString(o);
            } catch (JsonProcessingException ex) {
                return "[ERROR: Could not serialize object to JSON]";
            }
        }
    }

}
