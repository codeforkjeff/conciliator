package com.codefork.refine.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Simulates HTTP connections, returning mock connection objects
 * that return input stream objects for stored disk files.
 */
public class SimulatedConnectionFactory implements ConnectionFactory {

    protected Log log = LogFactory.getLog(this.getClass());

    private Map<String, String> urlsToFiles = new HashMap<>();

    public SimulatedConnectionFactory() {
        // periodically update these files with current output from the URLs,
        // and update test expectations accordingly

        urlsToFiles.put(
                "https://openlibrary.org/search.json?q=through%20the%20arc%20of%20the%20rainforest",
                "/openlibrary_rainforest.json");

        urlsToFiles.put(
                "http://pub.orcid.org/v1.2/search/orcid-bio/?rows=3&q=stephen%20hawking",
                "/orcid_stephen_hawking.xml");
        urlsToFiles.put(
                "http://pub.orcid.org/v1.2/search/orcid-bio/?rows=3&q=given-names:Igor%20AND%20family-name:Ozerov",
                "/orcid_igor_ozerov.xml");

        urlsToFiles.put(
                "http://localhost:8983/solr/test-core/select?wt=xml&q=The%20Complete%20Adventures%20of%20Sherlock%20Holmes&rows=3",
                "/solr_results.xml");

        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Jean-Fran%C3%A7ois%20Alexandre%201804%201874%22%20and%20local.sources%20%3D%20%22bnf%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/alexandre.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22nabokov%22%20and%20local.sources%20%3D%20%22nsk%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/nabokov_nsk.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22ncjecerence%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/nonsense.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22shakespeare%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22%20and%20local.sources%20%3D%20%22bnf%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare_bnf.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22hegel%22%20and%20local.sources%20%3D%20%22dnb%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare_dnb.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare_exact.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22%20and%20local.sources%20%3D%20%22lc%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare_lc.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22steinbeck%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/steinbeck_no_type.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22John%20Steinbeck%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/steinbeck_no_type.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22wittgenstein%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/wittgenstein.xml");
        urlsToFiles.put(
                "http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22wittgenstein%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/wittgenstein_personalnames.xml");
    }

    int numCalls = 0;

    @Override
    public HttpURLConnection createConnection(String url) throws IOException {
        numCalls++;
        log.info("Simulating request for " + url);
        if(urlsToFiles.containsKey(url)) {
            String resource = urlsToFiles.get(url);
            HttpURLConnection conn = mock(HttpURLConnection.class);
            InputStream is = getClass().getResourceAsStream(resource);
            if(is != null) {
                when(conn.getInputStream()).thenReturn(is);
            } else {
                String msg = "Resource file not found: " + resource;
                log.error(msg);
                throw new IOException(msg);
            }
            return conn;
        } else {
            String msg = "No test resource file found for URL: " + url;
            log.error(msg);
            throw new IOException(msg);
        }
    }

    public int getNumCallsToCreateConnection() {
        return numCalls;
    }

}
