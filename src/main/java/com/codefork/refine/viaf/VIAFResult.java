package com.codefork.refine.viaf;

import com.codefork.refine.NameType;

import java.util.ArrayList;
import java.util.List;

/**
 * The VIAFParser extracts relevant data from the VIAF XML into
 * VIAFResult instances.
 *
 * This is an "intermediate" data structure which needs to get
 * translated into the final format for OpenRefine to consume.
 */
public class VIAFResult {

    private String viafId;
    private NameType nameType;
    private List<NameEntry> nameEntries = new ArrayList<NameEntry>();

    public String getViafId() {
        return viafId;
    }

    public void setViafId(String viafId) {
        this.viafId = viafId;
    }

    public NameType getNameType() {
        return nameType;
    }

    public void setNameType(NameType nameType) {
        this.nameType = nameType;
    }

    public List<NameEntry> getNameEntries() {
        return nameEntries;
    }

    public void setNameEntries(List<NameEntry> nameEntries) {
        this.nameEntries = nameEntries;
    }

    public void addNameEntry() {
        nameEntries.add(new NameEntry());
    }

    public NameEntry getLastNameEntry() {
        return nameEntries.get(nameEntries.size() - 1);
    }

    public String getNameBySource(String sourceArg) {
        for(NameEntry nameEntry : getNameEntries()) {
            for(NameSource nameSource : nameEntry.getNameSources()) {
                if(nameSource.getCode().equals(sourceArg)) {
                    return nameEntry.getName();
                }
            }
        }
        return null;
    }

    /**
     * Get the name's ID according to specified source
     * @param sourceArg
     * @return
     */
    public String getSourceId(String sourceArg) {
        for(NameEntry nameEntry : getNameEntries()) {
            for(NameSource nameSource : nameEntry.getNameSources()) {
                if(nameSource.getCode().equals(sourceArg)) {
                    return nameSource.getSourceId();
                }
            }
        }
        return null;
    }

    /**
     * Get the name's VIAF source ID (of the form "ORG|ID")
     * @param sourceArg
     * @return
     */
    public String getViafSourceId(String sourceArg) {
        for(NameEntry nameEntry : getNameEntries()) {
            for(NameSource nameSource : nameEntry.getNameSources()) {
                if(nameSource.getCode().equals(sourceArg)) {
                    return nameSource.getViafSourceId();
                }
            }
        }
        return null;
    }

    public String getExactNameOrMostCommonName(String nameArg) {
        int count = 0;
        String name = null;
        for(NameEntry nameEntry : getNameEntries()) {
            if(nameEntry.getName().equals(nameArg)) {
                return nameEntry.getName();
            }
            if (nameEntry.getNameSources().size() > count) {
                name = nameEntry.getName();
                count = nameEntry.getNameSources().size();
            }
        }
        return name;
    }

}
