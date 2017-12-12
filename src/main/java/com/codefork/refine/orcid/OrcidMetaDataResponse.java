package com.codefork.refine.orcid;

import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.resources.View;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrcidMetaDataResponse extends ServiceMetaDataResponse {

    private final static String IDENTIFIER_SPACE = "http://xmlns.com/foaf/0.1/";
    private final static View VIEW = new View("https://orcid.org/{{id}}");
    private final static String SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private final static List<NameType> DEFAULT_TYPES = new ArrayList<>();

    static {
        DEFAULT_TYPES.add(new NameType("/people/person", "Person"));
    }

    public OrcidMetaDataResponse(String serviceName) {
        setName(serviceName);
        setIdentifierSpace(IDENTIFIER_SPACE);
        setSchemaSpace(SCHEMA_SPACE);
        setView(VIEW);
        setDefaultTypes(DEFAULT_TYPES);
    }

}
