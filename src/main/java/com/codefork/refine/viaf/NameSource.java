package com.codefork.refine.viaf;

/**
 * Encapsulation for the Source and ID for a name
 */
public class NameSource {

    private String code;
    private String id;

    public NameSource(String source, String id) {
        this.code = source;
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
