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
                log.info("Loading configuration from " + CONFIG_FILENAME);
                properties.load(new FileInputStream(CONFIG_FILENAME));
            } catch (IOException ex) {
                log.error("Error reading config file, skipping it: " + ex);
            }
        }
    }

    public Config(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns the properties relevant to a given data source name,
     * with the datasource.NAME prefix stripped off.
     *
     * e.g.
     * datasource.mysource.timeout=2000
     *
     * the Properties object returned will have key "timeout" with value "2000"
     *
     * @param name
     * @return
     */
    public Properties getDataSourceProperties(String name) {
        Properties props = new Properties();
        for(String key : getProperties().stringPropertyNames()) {
            String prefix = "datasource." + name;
            if(key.startsWith(prefix)) {
                String shortenedKey = key.substring(prefix.length()).replaceAll("^\\.+", "");
                if(shortenedKey.length() > 0) {
                    props.setProperty(shortenedKey, getProperties().getProperty(key));
                }
            }
        }
        return props;
    }

}
