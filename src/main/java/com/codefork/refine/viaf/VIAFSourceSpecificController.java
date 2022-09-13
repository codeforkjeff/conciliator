package com.codefork.refine.viaf;

import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.viaf.VIAF;
import com.codefork.refine.viaf.VIAFMetaDataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Doesn't inherit from DataSourceController or implement API interfaces
 * because of the additional source param. Ugh.
 */
@Controller
@RequestMapping("/reconcile/viaf/{source}")
@CrossOrigin(origins = "*")
public class VIAFSourceSpecificController {

    @Autowired
    VIAF viaf;

    // These /reconcile/viaf/{source} mappings are gross; it would be better to handle
    // separate controller in a separate controller, but we can't subclass VIAF b/c
    // we need the RequestMappings to go to new methods containing the additional source arg.

    @RequestMapping(value = { "", "/" })
    @ResponseBody
    public VIAFMetaDataResponse sourceSpecificServiceMetaData(
            HttpServletRequest request,
            @PathVariable String source) {
        String baseUrl = request.getRequestURL().toString();
        return new VIAFMetaDataResponse("VIAF", source, baseUrl);
    }

    @RequestMapping(value = { "", "/" }, params = "query")
    @ResponseBody
    public SearchResponse sourceSpecificQuerySingle(
            @PathVariable String source, @RequestParam(value = "query") String query) {
        return viaf.querySingle(query, new VIAF.SourceSpecificSearchQueryFactory(source));
    }

    @RequestMapping(value = { "", "/" }, params = "queries")
    @ResponseBody
    public Map<String, SearchResponse> sourceSpecificQueryMultiple(
            @PathVariable String source, @RequestParam(value = "queries") String queries) {
        return viaf.queryMultiple(queries, new VIAF.SourceSpecificSearchQueryFactory(source));
    }

}
