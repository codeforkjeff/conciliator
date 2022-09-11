package com.codefork.refine.datasource;

import com.codefork.refine.Config;
import com.codefork.refine.ExtensionQuery;
import com.codefork.refine.PropertyValueIdAndSettings;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchQueryFactory;
import com.codefork.refine.datasource.stats.Stats;
import com.codefork.refine.resources.CellList;
import com.codefork.refine.resources.ColumnMetaData;
import com.codefork.refine.resources.ExtensionResponse;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.ProposePropertiesResponse;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    private Config config;

    private Stats stats;

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

    @Autowired
    public DataSource(Config config, Stats stats) {
        setConfig(config);

        this.stats = stats;
        this.stats.setDataSourceName(getName());

        Properties props = getConfigProperties();

        if(props.containsKey("name")) {
            setName(props.getProperty("name"));
        }
    }

    @PreDestroy
    public void shutdown() {
        // no-op
    }

    @ExceptionHandler(ServiceNotImplementedException.class)
    public ResponseEntity serviceNotImplemented(ServiceNotImplementedException ex, HttpServletRequest request) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_IMPLEMENTED);
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

    public Stats getStats() {
        return stats;
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
    public abstract ServiceMetaDataResponse createServiceMetaDataResponse(String baseUrl);

    public SearchQueryFactory getSearchQueryFactory() {
        return defaultSearchQueryFactory;
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

            Map<String, SearchQuery> queriesMap = new HashMap<>();
            queriesMap.put("q0", searchQuery);

            Map<String, SearchResponse> resultsMap = search(queriesMap);

            return new SearchResponse(resultsMap.get("q0").getResult());
        } catch (JsonProcessingException jse) {
            log.error("Got an error processing JSON: " + jse.toString());
        }
        return null;
    }

    public Map<String, SearchResponse> queryMultiple(String queries, SearchQueryFactory searchQueryFactory) {
        log.debug("queries=" + queries);
        try {
            JsonNode root = mapper.readTree(queries);

            Map<String, SearchQuery> queriesMap = new HashMap<>();

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
        }
        return null;
    }

    public ProposePropertiesResponse proposeProperties(String type, int limit)
            throws ServiceNotImplementedException {
        throw new ServiceNotImplementedException(
                String.format("propose properties service not implemented for %s data source",
                        getName()));
    }

    public ExtensionResponse extend(ExtensionQuery query) throws ServiceNotImplementedException {
        Map<String, CellList> rows = new HashMap<>();
        for(String id : query.getIds()) {
            rows.put(id, extend(id, query.getProperties()));
        }

        // TODO: move column metadata generation into separate method
        List<ColumnMetaData> meta = new ArrayList<>();
        for(PropertyValueIdAndSettings prop : query.getProperties()) {
            ColumnMetaData col = new ColumnMetaData();
            col.setId(prop.getId());
            col.setName("isbn");
            col.setType(new NameType("isbn", "isbn"));
            meta.add(col);
        }

        ExtensionResponse<String> response = new ExtensionResponse<>();
        response.setMeta(meta);
        response.setRows(rows);

        log.debug(String.format("response=%s", new DeferredJSON(response)));

        return response;
    }

    /**
     * Subclasses should override and implement.
     * @param id record id
     * @param idsAndSettings list of property IDs and their settings, to be fetched
     * @return a list of cells for the passed-in properties
     * @throws ServiceNotImplementedException
     */
    public CellList extend(String id, List<PropertyValueIdAndSettings> idsAndSettings) throws ServiceNotImplementedException {
        throw new ServiceNotImplementedException(
                String.format("extend service not implemented for %s data source",
                        getName()));
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
