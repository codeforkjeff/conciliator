package com.codefork.refine.viaf;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.Result;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the main API for doing VIAF searches.
 */
@Service
public class VIAF {

    Log log = LogFactory.getLog(VIAF.class);
    private final VIAFService viafService;

    @Autowired
    public VIAF(VIAFService viafService) {
        this.viafService = viafService;
    }

    /**
     * Performs a search.
     * @param query search to perform
     * @return list of search results (a 0-size list if none, or if errors occurred)
     */
    public List<Result> search(SearchQuery query) {
        List<Result> results = new ArrayList<Result>();
        InputStream response = viafService.doSearch(query.createCqlQueryString(), query.getLimit());

        if(response != null) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            try {
                SAXParser parser = spf.newSAXParser();
                VIAFParser viafParser = new VIAFParser();

                long start = System.currentTimeMillis();
                parser.parse(response, viafParser);
                long parseTime = System.currentTimeMillis() - start;

                try {
                    response.close();
                } catch(IOException ioe) {
                    log.error("Ignoring error from trying to close connection input stream: " + ioe);
                }

                for (VIAFResult viafResult : viafParser.getResults()) {
                    /*
                    log.debug("Result=" + viafResult.getViafId());
                    log.debug("NameType=" + viafResult.getNameType().getViafCode());
                    for(NameEntry nameEntry : viafResult.getNameEntries()) {
                        log.debug("Name=" + nameEntry.getName());
                        log.debug("Sources=" + StringUtils.collectionToDelimitedString(nameEntry.getSources(), ","));
                    }
                    */

                    // if no explicit source was specified, we should use any exact
                    // match if present, otherwise the most common one
                    String name = query.getSource() != null ?
                            viafResult.getNameBySource(query.getSource()) :
                            viafResult.getExactNameOrMostCommonName(query.getQuery());
                    boolean exactMatch = name != null ? name.equals(query.getQuery()) : false;

                    results.add(new Result(
                            viafResult.getViafId(),
                            name,
                            viafResult.getNameType(),
                            // TODO: how would we calculate a score?
                            1,
                            exactMatch));
                }
                log.debug(String.format("Query: %s - parsing took %dms, got %d results",
                        query.getQuery(), parseTime, viafParser.getResults().size()));
            } catch (ParserConfigurationException ex) {
                log.error("error creating parser: " + ex);
            } catch (SAXException ex) {
                log.error("sax error: " + ex);
            } catch (IOException ex) {
                log.error("ioerror parsing: " + ex);
            }
        }

        return results;
    }

}
