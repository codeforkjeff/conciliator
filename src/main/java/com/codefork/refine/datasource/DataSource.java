package com.codefork.refine.datasource;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * A reconciliation data source.
 */
public abstract class DataSource {

    private String name = this.getClass().getSimpleName();
    private String configName;

    private Config config;

    public void init(Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
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
     * This provides a way for data sources to create and pass additional arbitrary data
     * to the controller.
     * @param request
     * @return
     */
    public Map<String, String> parseRequestToExtraParams(HttpServletRequest request) {
        return Collections.EMPTY_MAP;
    }

    /**
     * This is the main entry point for running a set of queries contained in a HTTP request.
     */
    public abstract Map<String, SearchResponse> search(Map<String, SearchQuery> queryEntries);

    /**
     * Returns the service metadata that OpenRefine uses on its first request
     * to the service.
     */
    public abstract ServiceMetaDataResponse createServiceMetaDataResponse(Map<String, String> extraParams);

}
