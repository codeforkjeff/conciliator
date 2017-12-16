package com.codefork.refine.orcid;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.Result;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("orcid")
public class Orcid extends OrcidBase {

    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        List<Result> results = searchKeyword(query);
        return results;
    }

}
