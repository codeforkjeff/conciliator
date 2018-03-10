package com.codefork.refine.orcid;

import com.codefork.refine.parsers.ParseState;
import com.codefork.refine.parsers.xml.EndElementHandler;
import com.codefork.refine.parsers.xml.StartElementHandler;
import com.codefork.refine.parsers.xml.XMLParser;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrcidIndividualRecordParser extends XMLParser<ParseState> {

    private final static Map<String, StartElementHandler<ParseState>> staticStartElementHandlers = new HashMap<>();
    private final static Map<String, EndElementHandler<ParseState>> staticEndElementHandlers  = new HashMap<>();

    static {
        final List<NameType> nameTypes = new ArrayList<>();
        nameTypes.add(new NameType("/people/person", "Person"));

        StartElementHandler<ParseState> captureHandler = new StartElementHandler<ParseState>() {
            public void handle(ParseState parseResult, String uri, String localName, String qName, Attributes attributes) {
                parseResult.captureChars = true;
            }
        };

        staticStartElementHandlers.put("record/person/name/given-names",
                captureHandler);

        staticStartElementHandlers.put("record/person/name/family-name",
                captureHandler);

        staticEndElementHandlers.put("record/person/name/given-names",
                new EndElementHandler<ParseState>() {
                    public void handle(ParseState parseState, String uri, String localName, String qName) {
                        parseState.result.setName(parseState.buf.toString());
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });

        staticEndElementHandlers.put("record/person/name/family-name",
                new EndElementHandler<ParseState>() {
                    public void handle(ParseState parseState, String uri, String localName, String qName) {
                        parseState.result.setName(parseState.result.getName() + " " + parseState.buf.toString());
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
    }

    public OrcidIndividualRecordParser(Result result) {
        super();
        this.startElementHandlers = staticStartElementHandlers;
        this.endElementHandlers = staticEndElementHandlers;
        getParseState().result = result;
    }

    @Override
    public ParseState createParseState() {
        return new ParseState();
    }

}
