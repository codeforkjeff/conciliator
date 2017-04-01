
package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.DataSource;
import com.codefork.refine.resources.SearchResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    private static final String BODY_404 = "<html>" +
            "<head><title>404 Not Found</title></head>" +
            "<body bgcolor=\"white\">" +
            "<h1>404 Not Found</h1>" +
            "<p>%s</p>" +
            "</body>" +
            "</html>";

    @Autowired
    public ReconcileController(Config config) {
        this.config = config;
        initDataSourceMap();
    }

    public void initDataSourceMap() {
        Map<String, DataSource> classNamesToDataSources = new HashMap<String, DataSource>();

        // defaults
        Properties props = new Properties();
        props.put("datasource.viaf", "com.codefork.refine.viaf.VIAF");
        props.put("datasource.viafproxy", "com.codefork.refine.viaf.VIAF");

        props.put("datasource.orcid", "com.codefork.refine.orcid.Orcid");
        props.put("datasource.orcid.name", "ORCID");

        props.put("datasource.openlibrary", "com.codefork.refine.openlibrary.OpenLibrary");
        props.put("datasource.openlibrary.name", "OpenLibrary");

        props.putAll(config.getProperties());

        String prefix = "datasource.";

        List<String> dataSourceNames = new ArrayList<String>();

        // figure out what the datasources are
        for(String propertyName : props.stringPropertyNames()) {
            if(propertyName.startsWith(prefix)) {
                String dataSourceName = propertyName.substring(prefix.length());
                if(dataSourceName.indexOf('.') == -1) {
                    dataSourceNames.add(dataSourceName);
                }
            }
        }

        for(String dataSourceName : dataSourceNames) {
            String baseKey = prefix + dataSourceName;
            String className = props.getProperty(baseKey);

            DataSource dataSource = null;
            // reuse dataSource object if already created
            if(classNamesToDataSources.containsKey(className)) {
                // note that reusing DataSources means we can't set unique names for them.
                dataSource = classNamesToDataSources.get(className);
            } else {
                try {
                    dataSource = (DataSource) Class.forName(className).newInstance();
                    dataSource.init(config);
                    String name = props.getProperty(baseKey + ".name");
                    if(name != null) {
                        dataSource.setName(name);
                    }
                    dataSource.setConfigName(dataSourceName);
                    classNamesToDataSources.put(className, dataSource);
                } catch(ClassNotFoundException e) {
                    log.error("ClassNotFoundException trying to instantiate object: " + className + " - " + e.toString());
                } catch(IllegalAccessException e) {
                    log.error("IllegalAccessException trying to instantiate object: " + className + " - " + e.toString());
                } catch(InstantiationException e) {
                    log.error("InstantiationException trying to instantiate object: " + className + " - " + e.toString());
                }
            }

            if(dataSource != null) {
                log.info(String.format("Registered data source '%s' (%s)", dataSourceName, className));
                dataSourceMap.put(dataSourceName, dataSource);
            }
        }
    }

    public DataSource getDataSource(String name) {
        return dataSourceMap.get(name);
    }

    public void shutdownDataSources() {
        for(DataSource dataSource: dataSourceMap.values()) {
            dataSource.shutdown();
        }
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
        if(parts.length >= 3) {
            String dataSourceStr = parts[2];

            DataSource dataSource = getDataSource(dataSourceStr);
            if(dataSource != null) {
                Map<String, String> extraParams = dataSource.parseRequestToExtraParams(request);

                return reconcile(dataSource, query, queries, extraParams);
            } else {
                return new ResponseEntity<String>(
                        String.format(BODY_404, "data source '" + dataSourceStr + "' not defined"), HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<String>(
                    String.format(BODY_404, "invalid URL path"), HttpStatus.NOT_FOUND);
        }
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
