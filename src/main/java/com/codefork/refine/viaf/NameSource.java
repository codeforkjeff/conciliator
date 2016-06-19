package com.codefork.refine.viaf;

/**
 * Encapsulation for the Source and ID for a name
 */
public class NameSource {

    // source. e.g. LC, BNF, etc.
    private String source;

    // the name's ID, parsed from the VIAF "sid" element consisting of "SOURCE|NAME_ID"
    private String nameId;

    // "actual" name ID according to the source institution itself
    private String sourceNameId;

    /**
     * @param sourceString string of form "SOURCE|NAME_ID" OR just a source alone
     */
    public NameSource(String sourceString) {
        parseSourceId(sourceString);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return string of form "SOURCE|NAME_ID"
     */
    public String getSourceId() {
        if(nameId == null) {
            return source;
        }
        return source + "|" + nameId;
    }

    /**
     * Parse a string of form "SOURCE|NAME_ID" into its parts,
     * storing them in separate fields in this object.
     * @param sourceId
     */
    public void parseSourceId(String sourceId) {
        if(sourceId != null) {
            String[] pieces = sourceId.split("\\|");
            if(pieces.length == 2) {
                source = pieces[0];
                nameId = pieces[1];
            } else if(pieces.length == 1) {
                source = sourceId;
            }
        }
    }

    /**
     * @return the name's ID, parsed from the VIAF "sid" element consisting of "SOURCE|NAME_ID"
     */
    public String getNameId() {
        return nameId;
    }

    public String getSourceNameId() {
        return sourceNameId;
    }

    public void setSourceNameId(String sourceNameId) {
        this.sourceNameId = sourceNameId;
    }
}
