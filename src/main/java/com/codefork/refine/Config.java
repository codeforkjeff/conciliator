package com.codefork.refine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Application configuration
 */
@Service
public class Config {

    public static final String DEFAULT_SERVICE_NAME = "VIAF Reconciliation Service";
    private static final String CONFIG_FILENAME = "refine_viaf.properties";

    private Log log = LogFactory.getLog(Config.class);
    private String serviceName = DEFAULT_SERVICE_NAME;
    private Properties properties = new Properties();

    public Config() {
        if(new File(CONFIG_FILENAME).exists()) {
            try {
                properties.load(new FileInputStream(CONFIG_FILENAME));
            } catch (IOException ex) {
                log.error("Error reading config file, skipping it: " + ex);
            }
        }
        setServiceName(properties.getProperty("service_name", getServiceName()));
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Properties getProperties() {
        return properties;
    }
}
