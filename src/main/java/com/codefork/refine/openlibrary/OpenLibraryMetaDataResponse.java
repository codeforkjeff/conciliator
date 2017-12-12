package com.codefork.refine.openlibrary;

import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.resources.View;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenLibraryMetaDataResponse extends ServiceMetaDataResponse {

    private final static String IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/user/hangy/viaf";
    private final static View VIEW = new View("https://openlibrary.org{{id}}");
    private final static String SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private final static List<NameType> DEFAULT_TYPES = new ArrayList<>();

    static {
        DEFAULT_TYPES.add(new NameType("/book/book", "Book"));
    }

    public OpenLibraryMetaDataResponse(String baseServiceName) {
        setName(baseServiceName);
        setIdentifierSpace(IDENTIFIER_SPACE);
        setSchemaSpace(SCHEMA_SPACE);
        setView(VIEW);
        setDefaultTypes(DEFAULT_TYPES);
    }

}
