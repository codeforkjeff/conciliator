package com.codefork.refine.controllers;

import com.codefork.refine.datasource.DataSource;
import com.codefork.refine.datasource.ServiceNotImplementedException;
import org.springframework.web.bind.annotation.RequestMapping;

public interface PreviewAPI {

    public static String PATH_PREVIEW = "/preview";

    DataSource getDataSource();

    /**
     * returns HTML
     * @return
     * @throws ServiceNotImplementedException
     */
    @RequestMapping(value = { PATH_PREVIEW }, params = "id")
    default Object preview() throws ServiceNotImplementedException {
        throw new ServiceNotImplementedException(
                String.format("Preview API not implemented for %s data source",
                        getDataSource().getName()));
    }

}
