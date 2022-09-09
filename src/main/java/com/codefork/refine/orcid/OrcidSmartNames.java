package com.codefork.refine.orcid;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchQueryFactory;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.datasource.stats.Stats;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.util.Collections;
import java.util.List;

@Component("orcidsmartnames")
public class OrcidSmartNames extends OrcidBase {

    SmartNamesModeSearchQueryFactory smartNamesModeSearchQueryFactory =
            new SmartNamesModeSearchQueryFactory();

    @Autowired
    public OrcidSmartNames(Config config, CacheManager cacheManager, ThreadPoolFactory threadPoolFactory, ConnectionFactory connectionFactory, Stats stats) {
        super(config, cacheManager, threadPoolFactory, connectionFactory, stats);
    }

    @Override
    public SearchQueryFactory getSearchQueryFactory() {
        return smartNamesModeSearchQueryFactory;
    }

    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        List<Result> results = Collections.emptyList();

        if(query.isOrcidSmartNamesMode()) {
            String name = query.getQuery();
            String[] namePieces = parseName(name);
            if(namePieces != null) {
                results = searchSmartNames(query, namePieces[0], namePieces[1]);
            }
        }
        if(results.isEmpty()) {
            results = searchKeyword(query);
        }
        return results;
    }

    private List<Result> searchSmartNames(SearchQuery query, String givenName, String familyName) throws Exception {
        String q = String.format("given-names:%s AND family-name:%s", givenName, familyName);
        String fields = createSearchFieldsQueryString(query);
        if(fields.length() > 0) {
            q += " " + fields;
        }
        String url = String.format("https://pub.orcid.org/v2.1/search/?rows=%d&q=", query.getLimit()) +
                UriUtils.encodeQueryParam(q, "UTF-8");
        return doSearch(query, url);
    }

    /**
     * Parse name into given name, family name parts. returns null if name is too complicated to be
     * parsed.
     * @param name
     * @return
     */
    public static String[] parseName(String name) {
        int numCommas = name.length() - name.replace(",", "").length();
        if(numCommas == 1) {
            String[] namePieces = name.split(",");
            if(namePieces.length == 2) {
                return new String[]{
                        namePieces[1].trim(),
                        namePieces[0].trim()
                };
            }
        } else {
            String[] namePieces = name.split("\\s+");
            if(namePieces.length == 2) {
                return namePieces;
            }
        }
        return null;
    }

    private static class SmartNamesModeSearchQueryFactory implements SearchQueryFactory {

        @Override
        public SearchQuery createSearchQuery(JsonNode queryStruct) {
            SearchQuery searchQuery = new SearchQuery(queryStruct);
            searchQuery.setOrcidSmartNamesMode(true);
            return searchQuery;
        }

        @Override
        public SearchQuery createSearchQuery(String query, int limit, NameType nameType, String typeStrict) {
            SearchQuery searchQuery = new SearchQuery(query, limit, nameType, typeStrict);
            searchQuery.setOrcidSmartNamesMode(true);
            return searchQuery;
        }
    }

}
