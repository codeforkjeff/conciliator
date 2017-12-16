package com.codefork.refine.controllers;

import com.codefork.refine.datasource.DataSource;

public class DataSourceController implements
        ReconciliationAPI, DataExtensionAPI, SuggestAPI, PreviewAPI {

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

}
