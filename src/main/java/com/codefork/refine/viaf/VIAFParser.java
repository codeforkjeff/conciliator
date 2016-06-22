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

    private static abstract class StartElementHandler {
        public abstract void handle(VIAFParser parser, String uri, String localName, String qName, Attributes attributes);
    }

    private static abstract class EndElementHandler {
        public abstract void handle(VIAFParser parser, String uri, String localName, String qName);
    }

    private static final Map<String, StartElementHandler> startElementHandlers = new HashMap<String, StartElementHandler>();
    private static final Map<String, EndElementHandler> endElementHandlers = new HashMap<String, EndElementHandler>();

    static {
        startElementHandlers.put("searchRetrieveResponse/records/record",
                new StartElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName, Attributes attributes) {
                        parser.result = new VIAFResult();
                    }
                });
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/sources",
                new StartElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName, Attributes attributes) {
                        parser.sourceIdMappings = new HashMap<String, String>();
                    }
                });
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings",
                new StartElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName, Attributes attributes) {
                        parser.nameEntries = new ArrayList<NameEntry>();
                    }
                });
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data",
                new StartElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName, Attributes attributes) {
                        parser.nameEntry = new NameEntry();
                    }
                });
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources",
                new StartElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName, Attributes attributes) {
                        parser.nameSources = new ArrayList<NameSource>();
                    }
                });
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/sources/source",
                new StartElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName, Attributes attributes) {
                        parser.nsidAttribute = attributes.getValue("nsid");
                        parser.captureChars = true;
                    }
                });

        StartElementHandler captureHandler = new StartElementHandler() {
            public void handle(VIAFParser parser, String uri, String localName, String qName, Attributes attributes) {
                parser.captureChars = true;
            }
        };

        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/nameType",
                captureHandler);
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/viafID",
                captureHandler);
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/text",
                captureHandler);
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/s",
                captureHandler);
        startElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/sid",
                captureHandler);

        endElementHandlers.put("searchRetrieveResponse/records/record",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        parser.associateSourceIds();

                        parser.results.add(parser.result);
                        parser.result = null;
                    }
                });
        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/nameType",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        parser.result.setNameType(NameType.getByViafCode(parser.buf.toString())) ;
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });
        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/viafID",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        parser.result.setViafId(parser.buf.toString());
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });
        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/sources/source",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        String sourceId = parser.buf.toString();
                        parser.sourceIdMappings.put(sourceId, parser.nsidAttribute);
                        parser.nsidAttribute = null;
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });
        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        parser.result.setNameEntries(parser.nameEntries);
                        parser.nameEntries = null;
                    }
                });
        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        parser.nameEntries.add(parser.nameEntry);
                        parser.nameEntry = null;
                    }
                });
        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        parser.nameEntry.setNameSources(parser.nameSources);
                        parser.nameSources = null;
                    }
                });
        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/text",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        parser.nameEntry.setName(parser.buf.toString());
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });
        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/s",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        String source = parser.buf.toString();
                        parser.nameSources.add(new NameSource(source));
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });

        endElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/sid",
                new EndElementHandler() {
                    public void handle(VIAFParser parser, String uri, String localName, String qName) {
                        /*
                        NOTE! The string in "sid" element is VIAF's own source ID,
                        in the format "SOURCE|NAME_ID". For example, the sid element
                        contains "LC|n  79081460" for John Steinbeck.

                        This name id is NOT ALWAYS the same as the record id used by the
                        source institution itself (though often, it is). In the case above,
                        the LC record ID for Steinbeck is "n79081460". The difference is
                        not always simply a matter of whitespace.

                        The mappings between this "sid" element and the ID used by the
                        source institution can be found at
                        "/records/record/recordData/VIAFCluster/sources"
                        which we store and associate at the end of each "record" element.
                        */

                        // has the form "SOURCE|NAME_ID"
                        String viafSourceId = parser.buf.toString();

                        String[] parts = viafSourceId.split("\\|");
                        if (parts.length == 2) {
                            String code = parts[0];

                            // check if Source object was already created from 's' element
                            boolean sourceAlreadyExists = false;
                            for (NameSource s : parser.nameSources) {
                                if (s.getSource().equals(code)) {
                                    s.parseSourceId(viafSourceId);
                                    sourceAlreadyExists = true;
                                    break;
                                }
                            }
                            if (!sourceAlreadyExists) {
                                parser.nameSources.add(new NameSource(viafSourceId));
                            }
                        } else {
                            System.out.println("ARGH, len of parts=" + parts.length);
                        }
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });
    }

    private boolean captureChars = false;

    private final Stack<String> path = new Stack<String>();

    private final List<VIAFResult> results = new ArrayList<VIAFResult>();
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

    private String getPath() {
        StringBuilder buf = new StringBuilder();
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

        StartElementHandler handler = startElementHandlers.get(path);
        if(handler != null) {
            handler.handle(this, uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        String path = getPath();
        //System.out.println(path);

        EndElementHandler handler = endElementHandlers.get(path);
        if(handler != null) {
            handler.handle(this, uri, localName, qName);
        }

        this.path.pop();
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if(captureChars) {
            buf.append(ch, start, length);
        }
    }

    /**
     * Associate VIAF's source IDs with name IDs from source institutions.
     * This is called at the end of each result, because the relevant data
     * was collected from different parts of the XML.
     */
    private void associateSourceIds() {
        for(NameEntry nameEntry : result.getNameEntries()) {
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
