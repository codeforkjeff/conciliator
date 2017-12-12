package com.codefork.refine.viaf;

import com.codefork.refine.resources.Extend;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.ProposeProperties;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.resources.View;

import java.util.ArrayList;
import java.util.List;

public class VIAFMetaDataResponse extends ServiceMetaDataResponse {

    private final static String IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/user/hangy/viaf";
    private final static View VIEW = new View("http://viaf.org/viaf/{{id}}");
    private final static String SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private final static List<NameType> DEFAULT_TYPES = new ArrayList<>();

    static {
        for(VIAFNameType nameType : VIAFNameType.values()) {
            DEFAULT_TYPES.add(nameType.asNameType());
        }
    }

    public VIAFMetaDataResponse(String baseServiceName, String source, String baseUrl) {
        setName(baseServiceName);
        if(source != null) {
            setName(getName() + " - " + source);
        }
        setIdentifierSpace(IDENTIFIER_SPACE);
        setSchemaSpace(SCHEMA_SPACE);
        setView(VIEW);
        setDefaultTypes(DEFAULT_TYPES);
        setExtend(new Extend(
                new ProposeProperties(baseUrl,
                        "/propose_properties")));
    }

}
