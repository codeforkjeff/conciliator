package com.codefork.refine.viaf;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a name record that we populate from the VIAF XML.
 */
public class NameEntry {
    private String name;
    private List<NameSource> nameSources = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NameSource> getNameSources() {
        return nameSources;
    }

    public void setNameSources(List<NameSource> nameSources) {
        this.nameSources = nameSources;
    }

}
