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

    private static final String CONFIG_FILENAME = "conciliator.properties";

    private Log log = LogFactory.getLog(Config.class);
    private Properties properties = new Properties();

    public Config() {
        if(new File(CONFIG_FILENAME).exists()) {
            try {
                properties.load(new FileInputStream(CONFIG_FILENAME));
            } catch (IOException ex) {
                log.error("Error reading config file, skipping it: " + ex);
            }
        }
    }

    public Properties getProperties() {
        return properties;
    }
}
