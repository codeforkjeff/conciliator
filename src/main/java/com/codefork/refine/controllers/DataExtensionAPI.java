package com.codefork.refine.controllers;

import com.codefork.refine.ExtensionQuery;
import com.codefork.refine.datasource.DataSource;
import com.codefork.refine.datasource.ServiceNotImplementedException;
import com.codefork.refine.resources.ExtensionResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;

public interface DataExtensionAPI {

    DataSource getDataSource();

    // Data Extension API
    // https://github.com/OpenRefine/OpenRefine/wiki/Data-Extension-API
    @RequestMapping(value = { "", "/" }, params = "extend")
    @ResponseBody
    default ExtensionResponse extend(@RequestParam(value = "extend") String extend)
            throws ServiceNotImplementedException {
        // TODO: convert json to ExtensionQuery
        return getDataSource().extend(new ExtensionQuery(new ArrayList<>(),
                new ArrayList<>()));
    }

}
