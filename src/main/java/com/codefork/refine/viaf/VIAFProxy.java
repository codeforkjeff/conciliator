package com.codefork.refine.viaf;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.SearchQueryFactory;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.SearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * VIAF proxy data source
 */
@Controller
@RequestMapping("/reconcile/viafproxy")
public class VIAFProxy extends VIAFBase {

    @RequestMapping(value = "/{source}")
    @ResponseBody
    public VIAFProxyModeMetaDataResponse proxyModeServiceMetaData(@PathVariable String source) {
        return new VIAFProxyModeMetaDataResponse(findNonViafSource(source));
    }

    @RequestMapping(value = "/{source}", params = "query")
    @ResponseBody
    public SearchResponse proxyModeQuerySingle(
            @PathVariable String source, @RequestParam(value = "query") String query) {
        return querySingle(query, new ProxyModeSearchQueryFactory(source));
    }

    @RequestMapping(value = "/{source}", params = "queries")
    @ResponseBody
    public Map<String, SearchResponse> proxyModeQueryMultiple(
            @PathVariable String source, @RequestParam(value = "queries") String queries) {
        return queryMultiple(queries, new ProxyModeSearchQueryFactory(source));
    }

    private static class ProxyModeSearchQueryFactory implements SearchQueryFactory {
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

}
