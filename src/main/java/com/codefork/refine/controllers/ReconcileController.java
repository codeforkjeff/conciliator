
package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.DataSource;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.viaf.VIAF;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Controller to handle all /reconcile/viaf paths.
 */
@Controller
@RequestMapping("/reconcile")
public class ReconcileController {

    private final ObjectMapper mapper = new ObjectMapper();

    Log log = LogFactory.getLog(ReconcileController.class);

    private final Config config;
    private final Map<String, DataSource> dataSourceMap = new HashMap<String, DataSource>();

    @Autowired
    public ReconcileController(Config config) {
        this.config = config;
        initDataSourceMap();
    }

    public void initDataSourceMap() {
        VIAF viaf = new VIAF();
        viaf.init(config);
        dataSourceMap.put("viaf", viaf);
        dataSourceMap.put("viafproxy", viaf);
    }

    public DataSource getDataSource(String name) {
        return dataSourceMap.get(name);
    }

    /**
     * Entry point for all reconciliation code
     */
    @RequestMapping(value = "/**")
    @ResponseBody
    public Object reconcile(
            HttpServletRequest request,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queries", required = false) String queries) {
        String path = request.getServletPath();
        String[] parts = path.split("/");
        String dataSourceStr = parts[2];

        DataSource dataSource = getDataSource(dataSourceStr);
        if(dataSource == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        Map<String, String> extraParams = dataSource.parseRequestToExtraParams(request);

        return reconcile(dataSource, query, queries, extraParams);
    }

    public Object reconcile(
            DataSource dataSource,
            String query,
            String queries,
            Map<String, String> extraParams) {

        if (query != null) {
            log.debug("query=" + query);
            try {
                SearchQuery searchQuery;
                if (query.startsWith("{")) {
                    JsonNode root = mapper.readTree(query);
                    searchQuery = SearchQuery.createFromJson(root, extraParams);
                } else {
                    searchQuery = new SearchQuery(query, 3, null, "should", extraParams);
                }

                Map<String, SearchQuery> queriesMap = new HashMap<String, SearchQuery>();
                queriesMap.put("q0", searchQuery);

                Map<String, SearchResponse> resultsMap = dataSource.search(queriesMap);

                return new SearchResponse(resultsMap.get("q0").getResult());
            } catch (JsonProcessingException jse) {
                log.error("Got an error processing JSON: " + jse.toString());
            } catch (IOException ioe) {
                log.error("Got IO error processing JSON: " + ioe.toString());
            }

        } else if (queries != null) {
            log.debug("queries=" + queries);
            try {
                JsonNode root = mapper.readTree(queries);

                Map<String, SearchQuery> queriesMap = new HashMap<String, SearchQuery>();

                for(Iterator<Map.Entry<String, JsonNode>> iter = root.fields(); iter.hasNext(); ) {
                    Map.Entry<String, JsonNode> fieldEntry = iter.next();

                    String indexKey = fieldEntry.getKey();
                    JsonNode queryStruct = fieldEntry.getValue();

                    SearchQuery searchQuery = SearchQuery.createFromJson(queryStruct, extraParams);
                    queriesMap.put(indexKey, searchQuery);
                }

                Map<String, SearchResponse> resultsMap = dataSource.search(queriesMap);

                log.debug(String.format("response=%s", new DeferredJSON(resultsMap)));

                return resultsMap;
            } catch (JsonProcessingException jse) {
                log.error("Got an error processing JSON: " + jse.toString());
            } catch (IOException ioe) {
                log.error("Got IO error processing JSON: " + ioe.toString());
            }
        }

        return dataSource.createServiceMetaDataResponse(extraParams);
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
