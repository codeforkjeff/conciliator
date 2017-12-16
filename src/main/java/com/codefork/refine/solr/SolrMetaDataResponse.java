package com.codefork.refine.solr;

import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.resources.View;
import com.codefork.refine.viaf.VIAFNameType;

import java.util.ArrayList;
import java.util.List;

public class SolrMetaDataResponse extends ServiceMetaDataResponse {

    private final static String IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/user/hangy/viaf";
    private final static String SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private final static List<NameType> DEFAULT_TYPES = new ArrayList<>();

    private View view;

    static {
        // TODO: it would be nice to make this list of default name types configurable
        // somehow; for now, just use the ones for VIAF
        for(VIAFNameType nameType : VIAFNameType.values()) {
            DEFAULT_TYPES.add(nameType.asNameType());
        }
    }

    public SolrMetaDataResponse(String serviceName, String viewUrl) {
        setName(serviceName);
        setIdentifierSpace(IDENTIFIER_SPACE);
        setSchemaSpace(SCHEMA_SPACE);
        setDefaultTypes(DEFAULT_TYPES);
        setView(new View(viewUrl));
    }

}
