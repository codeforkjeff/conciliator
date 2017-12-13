package com.codefork.refine.orcid;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/reconcile/orcid")
public class Orcid extends OrcidBase {

    @Override
    public ServiceMetaDataResponse createServiceMetaDataResponse(String baseUrl) {
        return new OrcidMetaDataResponse(getName());
    }

    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        List<Result> results = Collections.emptyList();
        results = searchKeyword(query);
        return results;
    }

}
