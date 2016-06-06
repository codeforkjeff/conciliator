package com.codefork.refine.resources;

import com.codefork.refine.Config;
import com.codefork.refine.viaf.sources.NonVIAFSource;

/**
 * Service metadata for "proxy mode": the important thing here is
 * "view" key contains URL template for the non-VIAF source (for example,
 * the LC website) rather than for VIAF.
 */
public class SourceMetaDataResponse extends ServiceMetaDataResponse {

    private String url;

    public SourceMetaDataResponse(Config config, NonVIAFSource nonVIAFSource) {
        super(config, nonVIAFSource.getCode());
        String source = nonVIAFSource.getCode();
        this.url = nonVIAFSource.getServiceURLTemplate();
        setName(source + " (by way of VIAF)");
    }

    @Override
    public ServiceMetaDataResponse.View getView() {
        return new ServiceMetaDataResponse.View(url);
    }

}
