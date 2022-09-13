package com.codefork.refine.viaf;

import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.viaf.VIAF;
import com.codefork.refine.viaf.VIAFProxyModeMetaDataResponse;
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
@CrossOrigin(origins = "*")
@RequestMapping("/reconcile/viafproxy/{source}")
public class VIAFProxyController {

    @Autowired
    VIAF viaf;

    @RequestMapping(value = { "", "/" })
    @ResponseBody
    public VIAFProxyModeMetaDataResponse proxyModeServiceMetaData(
            HttpServletRequest request,
            @PathVariable String source) {
        String baseUrl = request.getRequestURL().toString();
        return new VIAFProxyModeMetaDataResponse(viaf.findNonViafSource(source), baseUrl);
    }

    @RequestMapping(value = { "", "/" }, params = "query")
    @ResponseBody
    public SearchResponse proxyModeQuerySingle(
            @PathVariable String source, @RequestParam(value = "query") String query) {
        return viaf.querySingle(query, new VIAF.ProxyModeSearchQueryFactory(source));
    }

    @RequestMapping(value = { "", "/" }, params = "queries")
    @ResponseBody
    public Map<String, SearchResponse> proxyModeQueryMultiple(
            @PathVariable String source, @RequestParam(value = "queries") String queries) {
        return viaf.queryMultiple(queries, new VIAF.ProxyModeSearchQueryFactory(source));
    }

}
