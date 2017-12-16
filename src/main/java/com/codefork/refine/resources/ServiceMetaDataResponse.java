package com.codefork.refine.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Metadata about this reconciliation service
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ServiceMetaDataResponse {

    private String name;
    private String identifierSpace;
    private String schemaSpace;
    private View view;
    private List<NameType> defaultTypes;
    private Preview preview;
    private Extend extend;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifierSpace() {
        return identifierSpace;
    }

    public void setIdentifierSpace(String identifierSpace) {
        this.identifierSpace = identifierSpace;
    }

    public String getSchemaSpace() {
        return schemaSpace;
    }

    public void setSchemaSpace(String schemaSpace) {
        this.schemaSpace = schemaSpace;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public List<NameType> getDefaultTypes() {
        return defaultTypes;
    }

    public void setDefaultTypes(List<NameType> defaultTypes) {
        this.defaultTypes = defaultTypes;
    }

    public Preview getPreview() {
        return preview;
    }

    public void setPreview(Preview preview) {
        this.preview = preview;
    }

    public Extend getExtend() {
        return extend;
    }

    public void setExtend(Extend extend) {
        this.extend = extend;
    }

}
