package com.codefork.refine.viaf;

import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.viaf.sources.NonVIAFSource;

/**
 * Service metadata for "proxy mode": the important thing here is
 * "view" key contains URL template for the non-VIAF source (for example,
 * the LC website) rather than for VIAF.
 */
public class VIAFProxyModeMetaDataResponse extends VIAFMetaDataResponse {

    private String url;

    public VIAFProxyModeMetaDataResponse(NonVIAFSource nonVIAFSource) {
        super(nonVIAFSource.getCode() + " (by way of VIAF)", null);
        this.url = nonVIAFSource.getServiceURLTemplate();
    }

    @Override
    public ServiceMetaDataResponse.View getView() {
        return new ServiceMetaDataResponse.View(url);
    }

}
