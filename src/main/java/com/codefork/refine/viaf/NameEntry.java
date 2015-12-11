package com.codefork.refine.viaf;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a name record that we populate from the VIAF XML.
 */
public class NameEntry {
    private String name;
    private List<String> sources = new ArrayList<String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public void addSource(String source) {
        getSources().add(source);
    }
}
