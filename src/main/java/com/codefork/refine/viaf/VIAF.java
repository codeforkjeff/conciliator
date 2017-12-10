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

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * VIAF data source that supports sources
 */
@Controller
@RequestMapping("/reconcile/viaf")
public class VIAF extends VIAFBase {

    @RequestMapping(value = "/{source}")
    @ResponseBody
    public VIAFMetaDataResponse sourceSpecificServiceMetaData(
            HttpServletRequest request,
            @PathVariable String source) {
        String baseUrl = request.getRequestURL().toString();
        return new VIAFMetaDataResponse("VIAF", source, baseUrl);
    }

    @RequestMapping(value = "/{source}", params = "query")
    @ResponseBody
    public SearchResponse sourceSpecificQuerySingle(
            @PathVariable String source, @RequestParam(value = "query") String query) {
        return querySingle(query, new SourceSpecificSearchQueryFactory(source));
    }

    @RequestMapping(value = "/{source}", params = "queries")
    @ResponseBody
    public Map<String, SearchResponse> sourceSpecificQueryMultiple(
            @PathVariable String source, @RequestParam(value = "queries") String queries) {
        return queryMultiple(queries, new SourceSpecificSearchQueryFactory(source));
    }

    private static class SourceSpecificSearchQueryFactory implements SearchQueryFactory {
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

}
