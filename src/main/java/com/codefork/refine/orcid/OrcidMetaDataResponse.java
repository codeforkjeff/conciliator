package com.codefork.refine.orcid;

import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.viaf.VIAFNameType;

import java.util.ArrayList;
import java.util.List;

public class OrcidMetaDataResponse extends ServiceMetaDataResponse {

    private final static String IDENTIFIER_SPACE = "http://xmlns.com/foaf/0.1/";
    private final static View VIEW = new View("https://orcid.org/{{id}}");
    private final static String SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private final static List<NameType> DEFAULT_TYPES = new ArrayList<NameType>();

    static {
        DEFAULT_TYPES.add(new NameType("/people/person", "Person"));
    }

    public OrcidMetaDataResponse(String serviceName) {
        setName(serviceName);
    }

    @Override
    public String getIdentifierSpace() {
        return IDENTIFIER_SPACE;
    }

    @Override
    public String getSchemaSpace() {
        return SCHEMA_SPACE;
    }

    @Override
    public View getView() {
        return VIEW;
    }

    @Override
    public List<NameType> getDefaultTypes() {
        return DEFAULT_TYPES;
    }
}
