package com.codefork.refine.orcid;

import com.codefork.refine.Config;
import com.codefork.refine.PropertyValue;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.StringUtil;
import com.codefork.refine.ThreadPool;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.datasource.WebServiceDataSource;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.web.util.UriUtils;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This isn't very "abstract" since it is aware of smartnames mode.
 * But this exists to make it possible to have two subclasses that are
 * @Controllers for different urls.
 */
public abstract class OrcidBase extends WebServiceDataSource {

    Log log = LogFactory.getLog(Orcid.class);

    private SAXParserFactory spf = SAXParserFactory.newInstance();

    private final ThreadPool threadPoolForIndividualRecords;

    private static final int POOL_SIZE_FOR_INDIVIDUAL_RECORDS = 20;

    @Autowired
    public OrcidBase(Config config, CacheManager cacheManager, ThreadPoolFactory threadPoolFactory, ConnectionFactory connectionFactory) {
        super(config, cacheManager, threadPoolFactory, connectionFactory);
        threadPoolForIndividualRecords = createThreadPoolForIndividualRecords();

    }

    @Override
    protected ThreadPool createThreadPool() {
        return getThreadPoolFactory().getSharedThreadPool("orcid");
    }

    protected ThreadPool createThreadPoolForIndividualRecords() {

        ThreadPool pool = getThreadPoolFactory().getSharedThreadPool("orcid-individual-records");
        if(pool.getPoolSize() != POOL_SIZE_FOR_INDIVIDUAL_RECORDS) {
            pool.setPoolSize(POOL_SIZE_FOR_INDIVIDUAL_RECORDS);
        }
        return pool;
    }

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(String baseUrl) {
        return new OrcidMetaDataResponse(getName());
    }

    @Override
    public void shutdown() {
        super.shutdown();
        getThreadPoolFactory().releaseThreadPool(threadPoolForIndividualRecords);
    }

    protected static String createQueryString(SearchQuery query) {
        StringBuilder buf = new StringBuilder();
        buf.append(query.getQuery());
        String fields = createSearchFieldsQueryString(query);
        if(fields.length() > 0) {
            buf.append(" ");
            buf.append(fields);
        }
        return buf.toString();
    }

    /**
     * creates Solr-style "field:value" query string from properties in SearchQuery
     */
    protected static String createSearchFieldsQueryString(SearchQuery query) {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, PropertyValue> prop : query.getProperties().entrySet()) {
            if(!first) {
                buf.append(" ");
            }
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
            first = false;
        }
        return buf.toString();
    }


    protected List<Result> searchKeyword(SearchQuery query) throws Exception {
        String q = createQueryString(query);
        String url = String.format("https://pub.orcid.org/v2.1/search/?rows=%d&q=", query.getLimit()) +
                UriUtils.encodeQueryParam(q, "UTF-8");
        return doSearch(query, url);
    }

    protected List<Result> doSearch(SearchQuery query, String url) throws Exception {
        log.debug("Making request to " + url);
        HttpURLConnection conn = getConnectionFactory().createConnection(url);

        InputStream response = conn.getInputStream();

        SAXParser parser = spf.newSAXParser();
        OrcidSearchResultsParser orcidParser = new OrcidSearchResultsParser();

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

        return fillInResults(query, orcidParser.getResults());
    }

    class FillInResultTask implements Callable<Result> {

        private SearchQuery query;
        private Result result;

        public FillInResultTask(SearchQuery query, Result result) {
            this.query = query;
            this.result = result;
        }

        @Override
        public Result call() throws Exception {
            String url = String.format("https://pub.orcid.org/v2.1/%s/record", result.getId());

            log.debug("Filling in ORCID result: making request to " + url);

            HttpURLConnection conn = getConnectionFactory().createConnection(url);
            InputStream response = conn.getInputStream();

            SAXParser parser = spf.newSAXParser();
            OrcidIndividualRecordParser orcidParser = new OrcidIndividualRecordParser(result);

            parser.parse(response, orcidParser);

            try {
                response.close();
                conn.disconnect();
            } catch(IOException ioe) {
                log.error("Ignoring error from trying to close input stream and connection: " + ioe);
            }

            Result result = orcidParser.getParseState().result;

            if(result != null) {
                if(result.getName() != null) {
                    result.setScore(StringUtil.levenshteinDistanceRatio(result.getName(), query.getQuery()));
                } else {
                    log.warn("Name not found in record for " + result.getId());
                }
            }

            return result;
        }
    }

    /**
     * given a list of Results with id field populated, fills in the name and other fields
     * @param results
     * @return
     */
    private List<Result> fillInResults(SearchQuery query, List<Result> results) {
        List<FillInResultTask> tasks = new ArrayList<>();
        for (Result result : results) {
            tasks.add(new FillInResultTask(query, result));
        }

        List<Future<Result>> futures = new ArrayList<>();
        for (FillInResultTask task : tasks) {
            futures.add(threadPoolForIndividualRecords.submit(task));
        }

        List<Result> returnResults = new ArrayList<>();
        for (Future<Result> future : futures) {
            try {
                Result result = future.get();
                returnResults.add(result);
            } catch (InterruptedException | ExecutionException  e) {
                log.error("fillInResults: error getting value from future: " + StringUtil.getStackTrace(e));
            }
        }
        return returnResults;
    }

}
