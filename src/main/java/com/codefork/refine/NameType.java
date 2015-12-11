
package com.codefork.refine;

import com.codefork.refine.resources.VIAFNameType;

/**
 * Different types of names. Note that this is for the application's internal
 * use; see VIAFNameType (and the asVIAFNameType() method in this enum) for a
 * class used to format data sent to VIAF.
 */
public enum NameType {
    Person("/people/person", "Person", "Personal", "local.personalNames all \"%s\""),
    Organization("/organization/organization", "Corporate Name", "Corporate", "local.corporateNames all \"%s\""),
    Location("/location/location", "Geographic Name", "Geographic", "local.geographicNames all \"%s\""),
    // can't find better freebase ids for these two
    Book("/book/book", "Work", "UniformTitleWork", "local.uniformTitleWorks all \"%s\""),
    Edition("/book/book edition", "Expression", "UniformTitleExpression", "local.uniformTitleExpressions all \"%s\"");

    // ids are from freebase identifier ns
    private final String id;
    private final String displayName;
    private final String viafCode;
    private final String cqlString;

    NameType(String id, String displayName, String viafCode, String cqlString) {
       this.id = id;
       this.displayName = displayName;
       this.viafCode = viafCode;
       this.cqlString = cqlString;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getViafCode() {
        return viafCode;
    }

    public String getCqlString() {
        return cqlString;
    }

    public VIAFNameType asVIAFNameType() {
        return new VIAFNameType(getId(), getDisplayName());
    }

    public static NameType getByViafCode(String viafCodeArg) {
        for(NameType nameType: NameType.values()) {
            if(nameType.viafCode.equals(viafCodeArg)) {
                return nameType;
            }
        }
        return null;
    }

    public static NameType getById(String idArg) {
        for(NameType nameType: NameType.values()) {
            if(nameType.id.equals(idArg)) {
                return nameType;
            }
        }
        return null;
    }

}
