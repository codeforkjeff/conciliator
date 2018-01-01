package com.codefork.refine.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProposePropertiesResponse {

    public List<NameType> properties;
    public String type;
    public int limit;

    public List<NameType> getProperties() {
        return properties;
    }

    public void setProperties(List<NameType> properties) {
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
