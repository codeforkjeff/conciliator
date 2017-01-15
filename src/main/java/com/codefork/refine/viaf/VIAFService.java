package com.codefork.refine.viaf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Performs actual queries to the VIAF API.
 */
@Service
public class VIAFService {

    public static int TIMEOUT = 20000;

    Log log = LogFactory.getLog(VIAFService.class);

    /**
     * Do the search.
     * @param cql query in VIAF's CQL language
     * @param limit max number of results to return
     * @return HttpURLConnection if successful, null otherwise
     */
    public HttpURLConnection doSearch(String cql, int limit) throws IOException {
        String url = String.format("http://www.viaf.org/viaf/search?query=%s&sortKeys=holdingscount&maximumRecords=%s&httpAccept=application/xml",
                UriUtils.encodeQueryParam(cql, "UTF-8"), limit);
        log.debug("Making request to " + url);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        return connection;
    }

}
