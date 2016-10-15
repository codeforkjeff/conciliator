package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.viaf.sources.NonVIAFSource;

/**
 * Service metadata for "proxy mode": the important thing here is
 * "view" key contains URL template for the non-VIAF source (for example,
 * the LC website) rather than for VIAF.
 */
public class VIAFProxyModeMetaDataResponse extends VIAFMetaDataResponse {

    private String url;

    public VIAFProxyModeMetaDataResponse(String baseServiceName, NonVIAFSource nonVIAFSource) {
        super(baseServiceName, nonVIAFSource.getCode());
        this.url = nonVIAFSource.getServiceURLTemplate();
        setName(getName() + " (by way of VIAF)");
    }

    @Override
    public ServiceMetaDataResponse.View getView() {
        return new ServiceMetaDataResponse.View(url);
    }

}
