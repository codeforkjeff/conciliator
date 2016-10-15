package com.codefork.refine.viaf.sources;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.StringUtil;
import com.codefork.refine.resources.Result;
import com.codefork.refine.viaf.VIAF;
import com.codefork.refine.viaf.VIAFResult;

/**
 * This source is for VIAF itself.
 */
public class VIAFSource extends Source {

    @Override
    public String getServiceURLTemplate() {
        return "http://viaf.org/viaf/{{id}}";
    }

    @Override
    public Result formatResult(SearchQuery query, VIAFResult viafResult) {
        String source = query.getExtraParams().get(VIAF.EXTRA_PARAM_SOURCE_FROM_PATH);
        // if no explicit source was specified, we should use any exact
        // match if present, otherwise the most common one
        String name = source != null ?
                viafResult.getNameBySource(source) :
                viafResult.getExactNameOrMostCommonName(query.getQuery());
        boolean exactMatch = name != null ? name.equals(query.getQuery()) : false;

        Result r = new Result(
                viafResult.getViafId(),
                name,
                viafResult.getNameType(),
                StringUtil.levenshteinDistanceRatio(name, query.getQuery()),
                exactMatch);
        return r;
    }

}
