package com.codefork.refine;

import java.util.List;

public class ExtensionQuery {
    private List<String> ids;
    private List<PropertyValueIdAndSettings> properties;

    public ExtensionQuery(List<String> ids, List<PropertyValueIdAndSettings> properties) {
        this.ids = ids;
        this.properties = properties;
    }

    public List<String> getIds() {
        return ids;
    }

    public List<PropertyValueIdAndSettings> getProperties() {
        return properties;
    }
}
