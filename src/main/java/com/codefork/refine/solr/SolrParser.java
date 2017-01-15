package com.codefork.refine.solr;

import com.codefork.refine.parsers.xml.XMLParser;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import com.codefork.refine.parsers.xml.EndElementHandler;
import com.codefork.refine.parsers.xml.StartElementHandler;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrParser extends XMLParser<SolrParseState> {

    private final static Map<String, StartElementHandler<SolrParseState>> staticStartElementHandlers = new HashMap<String, StartElementHandler<SolrParseState>>();
    private final static Map<String, EndElementHandler<SolrParseState>> staticEndElementHandlers  = new HashMap<String, EndElementHandler<SolrParseState>>();

    static {

        staticStartElementHandlers.put("response/result/doc",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.result = new Result();
                        parseState.result.setType(parseState.nameTypes);
                    }
                });

        staticEndElementHandlers.put("response/result/doc/str",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName) {
                        String s = parseState.buf.toString();
                        if (SolrParseState.Field.ID.equals(parseState.fieldBeingCaptured)) {
                            parseState.result.setId(s);
                        } else if (SolrParseState.Field.NAME.equals(parseState.fieldBeingCaptured)) {
                            parseState.result.setName(s);
                        }
                        parseState.fieldBeingCaptured = null;
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });

        staticEndElementHandlers.put("response/result/doc/float",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName) {
                        String s = parseState.buf.toString();
                        parseState.result.setScore(Double.valueOf(s));
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });

        staticEndElementHandlers.put("response/result/doc",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName) {
                        parseState.results.add(parseState.result);
                        parseState.result = null;
                    }
                });
    }

    public String fieldId;
    public String fieldName;

    /**
     * @param fieldId solr fieldname to use for 'id' field in reconciliation result
     * @param fieldName solr fieldname to use for 'name' field in reconciliation result
     * @param nameType all records parsed from Solr will have this nameType
     */
    public SolrParser(String fieldId, String fieldName, NameType nameType) {
        super();
        this.startElementHandlers = staticStartElementHandlers;
        this.endElementHandlers = staticEndElementHandlers;
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.getParseState().nameTypes.add(nameType);

        this.startElementHandlers.put("response/result/doc/str",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        if (SolrParser.this.fieldId.equals(attributes.getValue("name"))) {
                            parseState.fieldBeingCaptured = SolrParseState.Field.ID;
                        } else if(SolrParser.this.fieldName.equals(attributes.getValue("name"))) {
                            parseState.fieldBeingCaptured = SolrParseState.Field.NAME;
                        }
                        if(parseState.fieldBeingCaptured != null) {
                            parseState.captureChars = true;
                        }
                    }
                });

        this.startElementHandlers.put("response/result/doc/float",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        if ("score".equals(attributes.getValue("name"))) {
                            parseState.captureChars = true;
                        }
                    }
                });

    }

    @Override
    public SolrParseState createParseState() {
        return new SolrParseState();
    }

}
