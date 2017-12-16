package com.codefork.refine.controllers;

import com.codefork.refine.datasource.DataSource;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface ReconciliationAPI {

    DataSource getDataSource();

    @RequestMapping(value = { "", "/" })
    @ResponseBody
    default ServiceMetaDataResponse serviceMetaData(HttpServletRequest request) {
        return getDataSource().createServiceMetaDataResponse(request.getRequestURL().toString());
    }

    @RequestMapping(value = { "", "/" }, params = "query")
    @ResponseBody
    default SearchResponse querySingle(@RequestParam(value = "query") String query) {
        return getDataSource().querySingle(query, getDataSource().getSearchQueryFactory());
    }

    @RequestMapping(value = { "", "/" }, params = "queries")
    @ResponseBody
    default Map<String, SearchResponse> queryMultiple(@RequestParam(value = "queries") String queries) {
        return getDataSource().queryMultiple(queries, getDataSource().getSearchQueryFactory());
    }

}
