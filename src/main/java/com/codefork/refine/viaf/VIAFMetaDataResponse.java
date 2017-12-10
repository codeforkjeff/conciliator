package com.codefork.refine.viaf;

import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.ServiceMetaDataResponse;

import java.util.ArrayList;
import java.util.List;

public class VIAFMetaDataResponse extends ServiceMetaDataResponse {

    private final static String IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/user/hangy/viaf";
    private final static View VIEW = new View("http://viaf.org/viaf/{{id}}");
    private final static String SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private final static List<NameType> DEFAULT_TYPES = new ArrayList<NameType>();

    static {
        for(VIAFNameType nameType : VIAFNameType.values()) {
            DEFAULT_TYPES.add(nameType.asNameType());
        }
    }

    public VIAFMetaDataResponse(String baseServiceName, String source) {
        setName(baseServiceName);
        if(source != null) {
            setName(getName() + " - " + source);
        }
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

    @Override
    public Extend getExtend() {
        return new Extend(
                new ProposeProperties("http://localhost:8080/reconcile/viaf",
                        "/propose_properties"));
    }

}
