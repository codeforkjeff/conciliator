package com.codefork.refine.resources;

import java.util.List;

/**
 * Metadata about this reconciliation service
 */
public abstract class ServiceMetaDataResponse {

    public static class View {
        private String url;

        public View(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    
    }
    
    private String name = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract String getIdentifierSpace();

    public abstract String getSchemaSpace();

    public abstract View getView();

    public abstract List<NameType> getDefaultTypes();

}
