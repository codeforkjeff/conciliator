package com.codefork.refine.viaf.sources;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.Result;
import com.codefork.refine.viaf.VIAFResult;

/**
 * Source instances contain code specific to each VIAF source (LC, BNF, etc).
 * We also model a VIAFSource so we can treat it just like any other source.
 */
public abstract class Source {

    /**
     * Returns this source organization's URL template, if it exists,
     * to be used for service metadata.
     * @return
     */
    public abstract String getServiceURLTemplate();

    /**
     * Do any necessary processing on ID string.
     * This exists because VIAF puts spaces in LC identifiers
     * that we need to strip in order for LC URLs to work.
     * @param id
     * @return
     */
    public String formatID(String id) {
        // by default, do nothing to the ID.
        return id;
    }

    /**
     * Format the data we got from VIAF into a Result object for JSONification.
     */
    public abstract Result formatResult(SearchQuery query, VIAFResult viafResult);

}
