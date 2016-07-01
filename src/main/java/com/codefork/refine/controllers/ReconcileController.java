
package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import com.codefork.refine.NameType;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchResult;
import com.codefork.refine.SearchTask;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.resources.SourceMetaDataResponse;
import com.codefork.refine.viaf.VIAF;
import com.codefork.refine.viaf.VIAFThreadPool;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    private final VIAFThreadPool viafThreadPool;
    private final Config config;

    @Autowired
    public ReconcileController(VIAF viaf, VIAFThreadPool viafThreadPool, Config config) {
        this.viaf = viaf;
        this.viafThreadPool = viafThreadPool;
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
     * TODO: DEPRECATED
     */
    @RequestMapping(value = "/throughviaf/{source}")
    @ResponseBody
    public Object reconcileProxyDeprecated(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queries", required = false) String queries,
            @PathVariable("source") String sourceFromPath) {
        return reconcile(query, queries, sourceFromPath, true);
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
                Future<SearchResult> future = viafThreadPool.submit(new SearchTask(viaf, "", searchQuery));
                List<Result> results = new ArrayList<Result>();
                try {
                    SearchResult searchResult = future.get();
                    results = searchResult.getResults();
                } catch(ExecutionException e) {
                    log.error("execution error: " + e.toString());
                } catch(InterruptedException e) {
                    log.error("interrupted error: " + e.toString());
                }
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

                long start = System.currentTimeMillis();

                List<SearchTask> tasks = new ArrayList<SearchTask>();

                for(Iterator<Map.Entry<String, JsonNode>> iter = root.fields(); iter.hasNext(); ) {
                    Map.Entry<String, JsonNode> fieldEntry = iter.next();

                    String indexKey = fieldEntry.getKey();
                    JsonNode queryStruct = fieldEntry.getValue();

                    SearchQuery searchQuery = createSearchQuery(queryStruct, source, proxyMode);

                    SearchTask task = new SearchTask(viaf, indexKey, searchQuery);
                    tasks.add(task);
                }

                List<Future<SearchResult>> futures = new ArrayList<Future<SearchResult>>();
                for(SearchTask task : tasks) {
                    futures.add(viafThreadPool.submit(task));
                }

                for(Future<SearchResult> future : futures) {
                    try {
                        SearchResult result = future.get();
                        String indexKey = result.getKey();
                        allResults.put(indexKey, new SearchResponse(result.getResults()));
                    } catch(ExecutionException e) {
                        log.error("error getting value from future: " + e);
                    }
                }
                // return empty arrays for searches that didn't complete due to errors
                for(Iterator<Map.Entry<String, JsonNode>> iter = root.fields(); iter.hasNext(); ) {
                    Map.Entry<String, JsonNode> fieldEntry = iter.next();
                    String indexKey = fieldEntry.getKey();
                    if(!allResults.containsKey(indexKey)) {
                        allResults.put(indexKey, new SearchResponse(new ArrayList<Result>()));
                    }
                }

                log.debug(String.format("%s tasks finished in %s", tasks.size(), System.currentTimeMillis() - start));

                log.debug(String.format("response=%s", new DeferredJSON(allResults)));

                return allResults;
            } catch (JsonProcessingException jse) {
                log.error("Got an error processing JSON: " + jse.toString());
            } catch (IOException ioe) {
                log.error("Got IO error processing JSON: " + ioe.toString());
            } catch (InterruptedException ex) {
                log.error("Executor interrupted while running tasks: " + ex.toString());
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
