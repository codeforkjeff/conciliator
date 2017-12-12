package com.codefork.refine.viaf;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.parsers.xml.EndElementHandler;
import com.codefork.refine.parsers.xml.StartElementHandler;
import com.codefork.refine.parsers.xml.XMLParser;
import com.codefork.refine.viaf.sources.Source;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * SAX parser handler. We use SAX b/c it's faster than loading a whole DOM.
 */
public class VIAFParser extends XMLParser<VIAFParseState> {

    private static final Map<String, StartElementHandler<VIAFParseState>> staticStartElementHandlers = new HashMap<String, StartElementHandler<VIAFParseState>>();
    private static final Map<String, EndElementHandler<VIAFParseState>> staticEndElementHandlers = new HashMap<String, EndElementHandler<VIAFParseState>>();

    static {
        staticStartElementHandlers.put("searchRetrieveResponse/records/record",
                new StartElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.viafResult = new VIAFResult();
                    }
                });
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/sources",
                new StartElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.sourceIdMappings = new HashMap<String, String>();
                    }
                });
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings",
                new StartElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.nameEntries = new ArrayList<NameEntry>();
                    }
                });
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data",
                new StartElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.nameEntry = new NameEntry();
                    }
                });
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources",
                new StartElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.nameSources = new ArrayList<NameSource>();
                    }
                });
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/sources/source",
                new StartElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.nsidAttribute = attributes.getValue("nsid");
                        parseState.captureChars = true;
                    }
                });

        StartElementHandler<VIAFParseState> captureHandler = new StartElementHandler<VIAFParseState>() {
            public void handle(VIAFParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                parseState.captureChars = true;
            }
        };

        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/nameType",
                captureHandler);
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/viafID",
                captureHandler);
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/text",
                captureHandler);
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/s",
                captureHandler);
        staticStartElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/sid",
                captureHandler);

        staticEndElementHandlers.put("searchRetrieveResponse/records/record",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        parseState.associateSourceIds();

                        parseState.viafResults.add(parseState.viafResult);
                        parseState.viafResult = null;
                    }
                });
        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/nameType",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        parseState.viafResult.setNameType(VIAFNameType.getByViafCode(parseState.buf.toString())) ;
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/viafID",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        parseState.viafResult.setViafId(parseState.buf.toString());
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/sources/source",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        String sourceId = parseState.buf.toString();
                        parseState.sourceIdMappings.put(sourceId, parseState.nsidAttribute);
                        parseState.nsidAttribute = null;
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        parseState.viafResult.setNameEntries(parseState.nameEntries);
                        parseState.nameEntries = null;
                    }
                });
        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        parseState.nameEntries.add(parseState.nameEntry);
                        parseState.nameEntry = null;
                    }
                });
        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        parseState.nameEntry.setNameSources(parseState.nameSources);
                        parseState.nameSources = null;
                    }
                });
        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/text",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        parseState.nameEntry.setName(parseState.buf.toString());
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/s",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
                        String source = parseState.buf.toString();
                        parseState.nameSources.add(new NameSource(source));
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });

        staticEndElementHandlers.put("searchRetrieveResponse/records/record/recordData/VIAFCluster/mainHeadings/data/sources/sid",
                new EndElementHandler<VIAFParseState>() {
                    public void handle(VIAFParseState parseState, String uri, String localName, String qName) {
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
                        String viafSourceId = parseState.buf.toString();

                        String[] parts = viafSourceId.split("\\|");
                        if (parts.length == 2) {
                            String code = parts[0];

                            // check if Source object was already created from 's' element
                            boolean sourceAlreadyExists = false;
                            for (NameSource s : parseState.nameSources) {
                                if (s.getSource().equals(code)) {
                                    s.parseSourceId(viafSourceId);
                                    sourceAlreadyExists = true;
                                    break;
                                }
                            }
                            if (!sourceAlreadyExists) {
                                parseState.nameSources.add(new NameSource(viafSourceId));
                            }
                        } else {
                            System.out.println("ARGH, len of parts=" + parts.length);
                        }
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
    }

    private Source source;
    private SearchQuery query;

    /**
     * @param source used for formatting results
     */
    public VIAFParser(Source source, SearchQuery query) {
        super();
        this.source = source;
        this.query = query;
        this.startElementHandlers = staticStartElementHandlers;
        this.endElementHandlers = staticEndElementHandlers;
    }

    @Override
    public VIAFParseState createParseState() {
        return new VIAFParseState();
    }

    @Override
    public void endDocument() {
        for (VIAFResult viafResult : parseState.viafResults) {
            /*
            log.debug("Result=" + viafResult.getViafId());
            log.debug("NameType=" + viafResult.getNameType().getViafCode());
            for(NameEntry nameEntry : viafResult.getNameEntries()) {
                log.debug("Name=" + nameEntry.getName());
                log.debug("Sources=" + StringUtils.collectionToDelimitedString(nameEntry.getSources(), ","));
            }
            */
            parseState.results.add(source.formatResult(query, viafResult));
        }
    }

}
