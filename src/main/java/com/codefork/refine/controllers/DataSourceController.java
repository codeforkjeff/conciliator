package com.codefork.refine.controllers;

import com.codefork.refine.datasource.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DataSourceController implements
        ReconciliationAPI, DataExtensionAPI, SuggestAPI, PreviewAPI {

    protected Log log = LogFactory.getLog(this.getClass());

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    public Log getLog() {
        return log;
    }

}
