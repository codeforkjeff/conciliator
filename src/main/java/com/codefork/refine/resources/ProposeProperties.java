package com.codefork.refine.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProposeProperties {
    @JsonProperty("service_url")
    private String serviceUrl;

    @JsonProperty("service_path")
    private String servicePath;

    public ProposeProperties(String serviceUrl, String servicePath) {
        this.serviceUrl = serviceUrl;
        this.servicePath = servicePath;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }
}
