package com.codefork.refine.datasource;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * A reconciliation data source.
 */
public abstract class DataSource {

    private Config config;

    public void init(Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
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

    public abstract Map<String, SearchResponse> search(Map<String, SearchQuery> queryEntries);

    public abstract ServiceMetaDataResponse createServiceMetaDataResponse(Map<String, String> extraParams);

}
