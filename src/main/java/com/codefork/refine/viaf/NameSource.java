package com.codefork.refine.viaf;

/**
 * Encapsulation for the Source and ID for a name
 */
public class NameSource {

    private String code;
    // VIAF's source ID, of the form "ORG|CODE"
    private String viafSourceId;
    // "actual" source ID according to the source institution itself
    private String sourceId;

    public NameSource(String source, String viafSourceId) {
        this.code = source;
        this.viafSourceId = viafSourceId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getViafSourceId() {
        return viafSourceId;
    }

    public void setViafSourceId(String viafSourceId) {
        this.viafSourceId = viafSourceId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
}
