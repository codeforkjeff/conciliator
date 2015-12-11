package com.codefork.refine;

/**
 * Application configuration
 */
public class Config {

    private String serviceName;

    public Config(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
