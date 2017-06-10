package com.codefork.refine.orcid;

import com.codefork.refine.PropertyValue;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.util.UriUtils;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Orcid extends WebServiceDataSource {
    Log log = LogFactory.getLog(Orcid.class);

    private SAXParserFactory spf = SAXParserFactory.newInstance();

    private static String createQueryString(SearchQuery query) {
        StringBuffer buf = new StringBuffer();
        buf.append(query.getQuery());
        for(Map.Entry<String, PropertyValue> prop : query.getProperties().entrySet()) {
            buf.append(" ");
            buf.append(prop.getKey());
            buf.append(":");
            String val = prop.getValue().asString();
            if(val.contains(" ")) {
                buf.append("\"");
                buf.append(val);
                buf.append("\"");
            } else {
                buf.append(val);
            }
        }
        return buf.toString();
    }

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(Map<String, String> extraParams) {
        return new OrcidMetaDataResponse(getName());
    }

    @Override
    public List<Result> search(SearchQuery query) throws Exception {

        String q = createQueryString(query);

        String url = String.format("http://pub.orcid.org/v1.2/search/orcid-bio/?rows=%d&q=", query.getLimit()) +
                UriUtils.encodeQueryParam(q, "UTF-8");
        log.debug("Making request to " + url);
        HttpURLConnection conn = getConnectionFactory().createConnection(url);

        InputStream response = conn.getInputStream();

        SAXParser parser = spf.newSAXParser();
        OrcidParser orcidParser = new OrcidParser();

        long start = System.currentTimeMillis();
        parser.parse(response, orcidParser);
        long parseTime = System.currentTimeMillis() - start;

        try {
            response.close();
            conn.disconnect();
        } catch(IOException ioe) {
            log.error("Ignoring error from trying to close input stream and connection: " + ioe);
        }

        log.debug(String.format("Query: %s - parsing took %dms, got %d results",
                query.getQuery(), parseTime, orcidParser.getResults().size()));

        return orcidParser.getResults();
    }
}
