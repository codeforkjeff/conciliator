
package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import com.codefork.refine.NameType;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.resources.SourceMetaDataResponse;
import com.codefork.refine.viaf.VIAF;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * Controller to handle all /reconcile/viaf paths.
 */
@Controller
@RequestMapping("/reconcile")
public class ReconcileController {
    /**
     * Time to wait for all search threads to finish in a single web request.
     */
    private static final int REQUEST_TIMEOUT_SECONDS = 10;

    private final ObjectMapper mapper = new ObjectMapper();

    Log log = LogFactory.getLog(ReconcileController.class);
    private final VIAF viaf;
    private final Config config;

    @Autowired
    public ReconcileController(VIAF viaf, Config config) {
        this.viaf = viaf;
        this.config = config;
    }

    /**
     * Endpoint that does non-source-specific reconciliation.
     */
    @RequestMapping(value = "/viaf")
    @ResponseBody
    public Object reconcileNoSource(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queries", required = false) String queries) {
        return reconcile(query, queries, null, false);
    }

    /**
     * Endpoint that does source-specific reconciliation.
     */
    @RequestMapping(value = "/viaf/{source}")
    @ResponseBody
    public Object reconcileWithSource(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queries", required = false) String queries,
            @PathVariable("source") String sourceFromPath) {
        return reconcile(query, queries, sourceFromPath, false);
    }

    /**
     * proxy mode URL
     */
    @RequestMapping(value = "/viafproxy/{source}")
    @ResponseBody
    public Object reconcileProxy(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queries", required = false) String queries,
            @PathVariable("source") String sourceFromPath) {
        return reconcile(query, queries, sourceFromPath, true);
    }

    /**
     * Entry point for all reconciliation code
     * @param query
     * @param queries
     * @param sourceFromPath
     * @param proxyMode
     * @return
     */
    private Object reconcile(
            String query,
            String queries,
            String sourceFromPath,
            boolean proxyMode) {
        String source = (sourceFromPath != null) ? sourceFromPath : null;

        if (query != null) {
            log.debug("query=" + query);
            try {
                SearchQuery searchQuery;
                if (query.startsWith("{")) {
                    JsonNode root = mapper.readTree(query);
                    searchQuery = createSearchQuery(root, source, proxyMode);
                } else {
                    searchQuery = new SearchQuery(query, 3, null, "should", proxyMode);
                }

                Map<String, SearchQuery> queriesMap = new HashMap<String, SearchQuery>();
                queriesMap.put("q0", searchQuery);

                Map<String, SearchResponse> resultsMap = viaf.search(queriesMap);

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

                long start = System.currentTimeMillis();

                Map<String, SearchQuery> queriesMap = new HashMap<String, SearchQuery>();

                for(Iterator<Map.Entry<String, JsonNode>> iter = root.fields(); iter.hasNext(); ) {
                    Map.Entry<String, JsonNode> fieldEntry = iter.next();

                    String indexKey = fieldEntry.getKey();
                    JsonNode queryStruct = fieldEntry.getValue();

                    SearchQuery searchQuery = createSearchQuery(queryStruct, source, proxyMode);
                    queriesMap.put(indexKey, searchQuery);
                }

                Map<String, SearchResponse> resultsMap = viaf.search(queriesMap);

                log.debug(String.format("%s tasks finished in %s (thread pool size=%s)", queriesMap.size(), System.currentTimeMillis() - start, viaf.getThreadPool().getPoolSize()));

                log.debug(String.format("response=%s", new DeferredJSON(resultsMap)));

                return resultsMap;
            } catch (JsonProcessingException jse) {
                log.error("Got an error processing JSON: " + jse.toString());
            } catch (IOException ioe) {
                log.error("Got IO error processing JSON: " + ioe.toString());
            }
        }

        if(proxyMode) {
            return new SourceMetaDataResponse(config, viaf.findNonViafSource(source));
        }
        return new ServiceMetaDataResponse(config, source);
    }

    /**
     * Factory method that builds SearchQuery instances out of the JSON structure
     * representing a single name query.
     * @param queryStruct a single name query
     * @param source two-letter source code
     * @return SearchQuery
     */
    private SearchQuery createSearchQuery(JsonNode queryStruct, String source, boolean proxyMode) {
        int limit = queryStruct.path("limit").asInt();
        if(limit == 0) {
            limit = 3;
        }

        NameType nameType = NameType.getById(queryStruct.path("type").asText());

        String typeStrict = null;
        if(!queryStruct.path("type_strict").isMissingNode()) {
            typeStrict = queryStruct.path("type_strict").asText();
        }

        SearchQuery searchQuery = new SearchQuery(
                queryStruct.path("query").asText().trim(),
                limit,
                nameType,
                typeStrict,
                proxyMode
                );

        if(source != null) {
            searchQuery.setSource(source);
        }

        return searchQuery;
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
