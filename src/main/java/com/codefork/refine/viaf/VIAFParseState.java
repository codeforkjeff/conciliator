package com.codefork.refine.viaf;

import com.codefork.refine.parsers.ParseState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Specialized ParseState object that stores a bunch of intermediate
 * data during parsing, which we need in order to generate the final results
 * for the OpenRefine client.
 */
public class VIAFParseState extends ParseState {

    public final List<VIAFResult> viafResults = new ArrayList<>();
    public VIAFResult viafResult = null;

    public List<NameEntry> nameEntries = null;
    public NameEntry nameEntry = null;
    public List<NameSource> nameSources = null;

    public Map<String, String> sourceIdMappings = null;
    public String nsidAttribute = null;

    public List<VIAFResult> getViafResults() {
        return viafResults;
    }

    /**
     * Associate VIAF's source IDs with name IDs from source institutions.
     * This is called at the end of each result, because the relevant data
     * was collected from different parts of the XML.
     */
    public void associateSourceIds() {
        for(NameEntry nameEntry : viafResult.getNameEntries()) {
            for(NameSource nameSource : nameEntry.getNameSources()) {
                String sourceNameId = sourceIdMappings.get(nameSource.getSourceId());
                if (sourceNameId == null) {
                    // sometimes ../mainHeadings/data/sources will list a source
                    // without an ID, but the XML will contain an ID under
                    // ../VIAFCluster/sources. This is the case for record 76304784,
                    // as of 6/17/2016.
                    //
                    // This code handles that case...
                    for (String k : sourceIdMappings.keySet()) {
                        if (k.contains("|")) {
                            String[] pieces = k.split("\\|");
                            if (pieces.length == 2) {
                                String orgCode = pieces[0];
                                String id = pieces[1];
                                if (orgCode.equals(nameSource.getSource())) {
                                    // since we have an ID now, find the NameSource and update it
                                    nameSource.parseSourceId(k);
                                    // also set the sourceNameId on NameSource
                                    sourceNameId = sourceIdMappings.get(k);
                                }
                            }
                        }
                    }
                }
                if (sourceNameId != null) {
                    nameSource.setSourceNameId(sourceNameId);
                }
            }
        }
        /*
        System.out.println("source id mappings=");
        for(String k : sourceIdMappings.keySet()) {
            System.out.println("k=" + k + "," + sourceIdMappings.get(k));
        }
        */
    }

}
