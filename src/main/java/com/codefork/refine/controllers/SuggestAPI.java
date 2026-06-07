package com.codefork.refine.controllers;

import com.codefork.refine.datasource.DataSource;
import com.codefork.refine.datasource.ServiceNotImplementedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

public  interface SuggestAPI {

    public static String PATH_SUGGEST_TYPE = "/suggest/type";
    public static String PATH_SUGGEST_PROPERTY = "/suggest/property";
    public static String PATH_SUGGEST_ENTITY = "/suggest/entity";

    DataSource getDataSource();

    @RequestMapping(value = { PATH_SUGGEST_ENTITY })
    @ResponseBody
    default Object suggestEntity() throws ServiceNotImplementedException {
        throw new ServiceNotImplementedException(
                "suggest entity service not implemented for %s data source".formatted(
                        getDataSource().getName()));
    }

    @RequestMapping(value = { PATH_SUGGEST_PROPERTY })
    @ResponseBody
    default Object suggestProperty() throws ServiceNotImplementedException {
        throw new ServiceNotImplementedException(
                "suggest property service not implemented for %s data source".formatted(
                        getDataSource().getName()));
    }

    @RequestMapping(value = { PATH_SUGGEST_TYPE })
    @ResponseBody
    default Object suggestType() throws ServiceNotImplementedException {
        throw new ServiceNotImplementedException(
                "suggest type service not implemented for %s data source".formatted(
                        getDataSource().getName()));
    }

}
