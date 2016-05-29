
package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import com.codefork.refine.NameType;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchThread;
import com.codefork.refine.resources.Result;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
     * "Through viaf" endpoint
     */
    @RequestMapping(value = "/throughviaf/{source}")
    @ResponseBody
    public Object reconcileThrough(
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
     * @param throughMode
     * @return
     */
    private Object reconcile(
            String query,
            String queries,
            String sourceFromPath,
            boolean throughMode) {

        String source = (sourceFromPath != null) ? sourceFromPath : null;

        if (query != null) {
            log.debug("query=" + query);
            try {
                SearchQuery searchQuery;
                if (query.startsWith("{")) {
                    JsonNode root = mapper.readTree(query);
                    searchQuery = createSearchQuery(root, source, throughMode);
                } else {
                    searchQuery = new SearchQuery(query, 3, null, "should", throughMode);
                }
                List<Result> results = viaf.search(searchQuery);
                return new SearchResponse(results);
            } catch (JsonProcessingException jse) {
                log.error("Got an error processing JSON: " + jse.toString());
            } catch (IOException ioe) {
                log.error("Got IO error processing JSON: " + ioe.toString());
            }

        } else if (queries != null) {
            log.debug("queries=" + queries);
            try {
                Map<String, SearchResponse> allResults = new HashMap<String, SearchResponse>();

                JsonNode root = mapper.readTree(queries);

                // NOTE: VIAF seems to have a limit of 6 simultaneous
                // requests. To be conservative, we default to 3.
                ExecutorService executor = Executors.newFixedThreadPool(3);
                Map<String, SearchThread> threads = new HashMap<String, SearchThread>();

                long start = System.currentTimeMillis();

                Iterator<Map.Entry<String, JsonNode>> iter = root.fields();
                while (iter.hasNext()) {
                    Map.Entry<String, JsonNode> fieldEntry = iter.next();

                    String indexKey = fieldEntry.getKey();
                    JsonNode queryStruct = fieldEntry.getValue();

                    SearchQuery searchQuery = createSearchQuery(queryStruct, source, throughMode);

                    SearchThread worker = new SearchThread(viaf, searchQuery);
                    executor.execute(worker);
                    threads.put(indexKey, worker);
                }

                executor.shutdown();
                executor.awaitTermination(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                log.debug(String.format("%s threads finished in %s", threads.size(), System.currentTimeMillis() - start));

                // collect results from the finished threads
                for (Map.Entry<String, SearchThread> threadStruct : threads.entrySet()) {
                    String indexKey = threadStruct.getKey();
                    SearchThread searchThread = threadStruct.getValue();
                    List<Result> results = searchThread.getResults();
                    allResults.put(indexKey, new SearchResponse(results));
                }

                log.debug(String.format("response=%s", new DeferredJSON(allResults)));

                return allResults;
            } catch (JsonProcessingException jse) {
                log.error("Got an error processing JSON: " + jse.toString());
            } catch (IOException ioe) {
                log.error("Got IO error processing JSON: " + ioe.toString());
            } catch (InterruptedException ex) {
                log.error("Interrupted while waiting for threads: " + ex.toString());
            }
        }

        if(throughMode) {
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
    private SearchQuery createSearchQuery(JsonNode queryStruct, String source, boolean throughMode) {
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
                throughMode
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
