package com.codefork.refine.openlibrary;

import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenLibraryMetaDataResponse extends ServiceMetaDataResponse {

    private final static String IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/user/hangy/viaf";
    private final static View VIEW = new View("https://openlibrary.org{{id}}");
    private final static String SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private final static List<NameType> DEFAULT_TYPES = new ArrayList<NameType>();

    static {
        DEFAULT_TYPES.add(new NameType("/book/book", "Book"));
    }

    public OpenLibraryMetaDataResponse(String baseServiceName) {
        setName(baseServiceName);
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
