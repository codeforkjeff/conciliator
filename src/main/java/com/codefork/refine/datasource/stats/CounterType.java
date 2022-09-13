package com.codefork.refine.datasource.stats;

public enum CounterType {
    QUERIES("countQueries"),
    ERRORS("countErrors");

    private final String jsonKeyName;

    private CounterType(String jsonKeyName) {
        this.jsonKeyName = jsonKeyName;
    }

    public String getJsonKeyName() {
        return jsonKeyName;
    }
}
