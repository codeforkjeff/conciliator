package com.codefork.refine.viaf;

import com.codefork.refine.NameType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * SAX parser handler. We use SAX b/c it's faster than loading a whole DOM.
 */
public class VIAFParser extends DefaultHandler {

    private boolean captureChars = false;

    private Stack<String> path = new Stack<String>();

    private List<VIAFResult> results = new ArrayList<VIAFResult>();
    private VIAFResult result = null;
    private List<NameEntry> nameEntries = null;
    private NameEntry nameEntry = null;
    private List<NameSource> nameSources = null;

    private Map<String, String> sourceIdMappings = null;
    private String nsidAttribute = null;

    /** buffer for collecting contents of an Element as parser does processing */
    private StringBuilder buf = new StringBuilder();

    public List<VIAFResult> getResults() {
        return results;
    }

    public String getPath() {
        StringBuffer buf = new StringBuffer();
        String delim = "";
        for(String part : path) {
            buf.append(delim);
            buf.append(part);
            delim = "/";
        }
        return buf.toString();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        // strip ns prefix
        String name = qName;
        if(name.contains(":")) {
            name = qName.split("\\:")[1];
        }
        path.push(name);

        String path = getPath();
        //System.out.println(path);

        if(path.equals(
                "searchRetrieveResponse/records/record")) {
            result = new VIAFResult();
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/sources")) {
            sourceIdMappings = new HashMap<String, String>();
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings")) {
            nameEntries = new ArrayList<NameEntry>();
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data")) {
            nameEntry = new NameEntry();
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources")) {
            nameSources = new ArrayList<NameSource>();
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/sources/source")) {
            nsidAttribute = attributes.getValue("nsid");
            captureChars = true;
        } else if(
            path.equals(
            "searchRetrieveResponse/records/record/recordData/VIAFCluster/nameType") ||
            path.equals(
            "searchRetrieveResponse/records/record/recordData/VIAFCluster/viafID") ||
            path.equals(
            "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/text") ||
            path.equals(
            "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/s") ||
            path.equals(
            "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/sid")) {
            captureChars = true;
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        String path = getPath();
        //System.out.println(path);

        if(path.equals(
                "searchRetrieveResponse/records/record")) {

            associateSourceIds();

            results.add(result);
            result = null;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/nameType")) {
            result.setNameType(NameType.getByViafCode(buf.toString())) ;
            buf = new StringBuilder();
            captureChars = false;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/viafID")) {
            result.setViafId(buf.toString());
            buf = new StringBuilder();
            captureChars = false;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/sources/source")) {
            sourceIdMappings.put(buf.toString(), nsidAttribute);
            nsidAttribute = null;
            buf = new StringBuilder();
            captureChars = false;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings")) {
            result.setNameEntries(nameEntries);
            nameEntries = null;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data")) {
            nameEntries.add(nameEntry);
            nameEntry = null;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources")) {
            nameEntry.setNameSources(nameSources);
            nameSources = null;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/text")) {
            nameEntry.setName(buf.toString());
            buf = new StringBuilder();
            captureChars = false;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/s")) {
            String source = buf.toString();
            nameSources.add(new NameSource(source, null));
            buf = new StringBuilder();
            captureChars = false;
        } else if(path.equals(
                "searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/sid")) {

            /*
            NOTE! The string in "sid" element is VIAF's own source identifier,
            in the format "ORG_ID|RECORD_ID". For example, the sid element
            contains "LC|n  79081460" for John Steinbeck.

            This record id is NOT ALWAYS the same as the record id used by the
            source institution itself (though often, it is). In the case above,
            the LC record ID for Steinbeck is "n79081460". The difference is
            not always simply a matter of whitespace.

            The mappings between this "sid" element and the ID used by the
            source institution can be found at
            "/records/record/recordData/VIAFCluster/sources"
            which we store and associate at the end of each "record" element.
            */

            // has the form "CODE|ID"
            String viafSourceId = buf.toString();

            String[] parts = viafSourceId.split("\\|");
            if (parts.length == 2) {
                String code = parts[0];

                // check if Source object was already created from 's' element
                boolean sourceAlreadyExists = false;
                for(NameSource s : nameSources) {
                    if(s.getCode().equals(code)) {
                        s.setViafSourceId(viafSourceId);
                        sourceAlreadyExists = true;
                        break;
                    }
                }
                if(!sourceAlreadyExists) {
                    nameSources.add(new NameSource(code, viafSourceId));
                }
            } else {
                System.out.println("ARGH, len of parts=" + parts.length);
            }
            buf = new StringBuilder();
            captureChars = false;
        }

        this.path.pop();
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if(captureChars) {
            buf.append(ch, start, length);
        }
    }

    private void associateSourceIds() {
        for(NameEntry nameEntry : result.getNameEntries()) {
            for(NameSource nameSource : nameEntry.getNameSources()) {
                String sourceId = sourceIdMappings.get(nameSource.getViafSourceId());
                if(sourceId != null) {
                    nameSource.setSourceId(sourceId);
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
