package com.codefork.refine.datasource;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface ConnectionFactory {

    HttpURLConnection createConnection(String url) throws IOException;

}
