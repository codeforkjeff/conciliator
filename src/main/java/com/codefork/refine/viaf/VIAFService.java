package com.codefork.refine.viaf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Performs actual queries to the VIAF API.
 */
@Service
public class VIAFService {

    Log log = LogFactory.getLog(VIAFService.class);

    /**
     * Do the search.
     * @param cql query in VIAF's CQL language
     * @param limit max number of results to return
     * @return InputStream if successful, null otherwise
     */
    public InputStream doSearch(String cql, int limit) {
        InputStream response = null;
        try {
            String url = String.format("http://www.viaf.org/viaf/search?query=%s&sortKeys=holdingscount&maximumRecords=%s&httpAccept=application/xml",
                    UriUtils.encodeQueryParam(cql, "UTF-8"), limit);
            log.debug("Making request to " + url);
            URLConnection connection = new URL(url).openConnection();
            response = connection.getInputStream();
        } catch(UnsupportedEncodingException ex) {
            log.error("error constructing VIAF url: " + ex);
        } catch (IOException ioe) {
            log.error("error opening connection to VIAF: " + ioe);
        }
        return response;
    }

}
