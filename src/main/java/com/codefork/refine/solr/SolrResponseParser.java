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

public class SolrResponseParser extends XMLParser<SolrParseState> {

    private final static Map<String, StartElementHandler<SolrParseState>> staticStartElementHandlers = new HashMap<String, StartElementHandler<SolrParseState>>();
    private final static Map<String, EndElementHandler<SolrParseState>> staticEndElementHandlers  = new HashMap<String, EndElementHandler<SolrParseState>>();

    static {
        final List<NameType> nameTypes = new ArrayList<NameType>();
        nameTypes.add(new NameType("/people/person", "Person"));

        staticStartElementHandlers.put("response/result/doc",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseResult, String uri, String localName, String qName, Attributes attributes) {
                        parseResult.result = new Result();
                        parseResult.result.setType(nameTypes);
                    }
                });

        staticEndElementHandlers.put("response/result/doc/str",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseResult, String uri, String localName, String qName) {
                        String s = parseResult.buf.toString();
                        if (SolrParseState.Field.ID.equals(parseResult.fieldBeingCaptured)) {
                            parseResult.result.setId(s);
                        } else if (SolrParseState.Field.NAME.equals(parseResult.fieldBeingCaptured)) {
                            parseResult.result.setName(s);
                        }
                        parseResult.fieldBeingCaptured = null;
                        parseResult.buf = new StringBuilder();
                        parseResult.captureChars = false;
                    }
                });

        staticEndElementHandlers.put("response/result/doc",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseResult, String uri, String localName, String qName) {
                        parseResult.results.add(parseResult.result);
                        parseResult.result = null;
                    }
                });
    }

    public String fieldId;
    public String fieldName;

    public SolrResponseParser(String fieldId, String fieldName) {
        super();
        this.startElementHandlers = staticStartElementHandlers;
        this.endElementHandlers = staticEndElementHandlers;
        this.fieldId = fieldId;
        this.fieldName = fieldName;

        this.startElementHandlers.put("response/result/doc/str",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseResult, String uri, String localName, String qName, Attributes attributes) {
                        if (SolrResponseParser.this.fieldId.equals(attributes.getValue("name"))) {
                            parseResult.fieldBeingCaptured = SolrParseState.Field.ID;
                        } else if(SolrResponseParser.this.fieldName.equals(attributes.getValue("name"))) {
                            parseResult.fieldBeingCaptured = SolrParseState.Field.NAME;
                        }
                        if(parseResult.fieldBeingCaptured != null) {
                            parseResult.captureChars = true;
                        }
                    }
                });
    }

    @Override
    public SolrParseState createParseResult() {
        return new SolrParseState();
    }

}
