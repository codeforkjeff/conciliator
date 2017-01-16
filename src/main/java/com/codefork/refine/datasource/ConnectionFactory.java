package com.codefork.refine.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionFactory {

    public static int TIMEOUT = 20000;

    Log log = LogFactory.getLog(ConnectionFactory.class);

    public HttpURLConnection createConnection(String url) throws IOException {
        log.debug("Making request to " + url);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        return connection;
    }

}
