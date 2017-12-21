package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchQueryFactory;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.NameType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * VIAF data source that supports sources
 */
@Component("viaf")
public class VIAF extends VIAFBase {

    public static class SourceSpecificSearchQueryFactory implements SearchQueryFactory {
        private String source;

        public SourceSpecificSearchQueryFactory(String source) {
            this.source = source;
        }

        @Override
        public SearchQuery createSearchQuery(JsonNode queryStruct) {
            SearchQuery searchQuery = new SearchQuery(queryStruct);
            searchQuery.setViafSource(source);
            return searchQuery;
        }

        @Override
        public SearchQuery createSearchQuery(String query, int limit, NameType nameType, String typeStrict) {
            SearchQuery searchQuery = new SearchQuery(query, limit, nameType, typeStrict);
            searchQuery.setViafSource(source);
            return searchQuery;
        }
    }

    public static class ProxyModeSearchQueryFactory implements SearchQueryFactory {
        private String source;

        public ProxyModeSearchQueryFactory(String source) {
            this.source = source;
        }

        @Override
        public SearchQuery createSearchQuery(JsonNode queryStruct) {
            SearchQuery searchQuery = new SearchQuery(queryStruct);
            searchQuery.setViafSource(source);
            searchQuery.setViafProxyMode(true);
            return searchQuery;
        }

        @Override
        public SearchQuery createSearchQuery(String query, int limit, NameType nameType, String typeStrict) {
            SearchQuery searchQuery = new SearchQuery(query, limit, nameType, typeStrict);
            searchQuery.setViafSource(source);
            searchQuery.setViafProxyMode(true);
            return searchQuery;
        }
    }

    @Autowired
    public VIAF(Config config, CacheManager cacheManager, ThreadPoolFactory threadPoolFactory, ConnectionFactory connectionFactory) {
        super(config, cacheManager, threadPoolFactory, connectionFactory);
    }

}

