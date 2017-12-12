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

/**
 * SAX parser handler. We use SAX b/c it's faster than loading a whole DOM.
 */
public class OrcidParser extends XMLParser<ParseState> {

    private final static Map<String, StartElementHandler<ParseState>> staticStartElementHandlers = new HashMap<String, StartElementHandler<ParseState>>();
    private final static Map<String, EndElementHandler<ParseState>> staticEndElementHandlers  = new HashMap<String, EndElementHandler<ParseState>>();

    static {
        final List<NameType> nameTypes = new ArrayList<NameType>();
        nameTypes.add(new NameType("/people/person", "Person"));

        staticStartElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result",
                new StartElementHandler<ParseState>() {
                    public void handle(ParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.result = new Result();
                        parseState.result.setType(nameTypes);
                    }
                });

        StartElementHandler<ParseState> captureHandler = new StartElementHandler<ParseState>() {
            public void handle(ParseState parseResult, String uri, String localName, String qName, Attributes attributes) {
                parseResult.captureChars = true;
            }
        };

        staticStartElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/relevancy-score",
                captureHandler);
        staticStartElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-identifier/path",
                captureHandler);
        staticStartElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/given-names",
                captureHandler);
        staticStartElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/family-name",
                captureHandler);

        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result",
                new EndElementHandler<ParseState>() {
                    public void handle(ParseState parseState, String uri, String localName, String qName) {
                        parseState.results.add(parseState.result);
                        parseState.result = null;
                    }
                });
        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/relevancy-score",
                new EndElementHandler<ParseState>() {
                    public void handle(ParseState parseState, String uri, String localName, String qName) {
                        parseState.result.setScore(Double.valueOf(parseState.buf.toString()));
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-identifier/path",
                new EndElementHandler<ParseState>() {
                    public void handle(ParseState parseState, String uri, String localName, String qName) {
                        parseState.result.setId(parseState.buf.toString());
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/given-names",
                new EndElementHandler<ParseState>() {
                    public void handle(ParseState parseState, String uri, String localName, String qName) {
                        parseState.result.setName(parseState.buf.toString());
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/family-name",
                new EndElementHandler<ParseState>() {
                    public void handle(ParseState parseState, String uri, String localName, String qName) {
                        String nameSoFar = "";
                        if(parseState.result.getName() != null) {
                            nameSoFar = parseState.result.getName();
                        }
                        String sep = "";
                        if(nameSoFar.length() > 0) {
                            sep = " ";
                        }
                        parseState.result.setName(nameSoFar + sep + parseState.buf.toString());
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });

    }

    public OrcidParser() {
        super();
        this.startElementHandlers = staticStartElementHandlers;
        this.endElementHandlers = staticEndElementHandlers;
    }

    @Override
    public ParseState createParseState() {
        return new ParseState();
    }

}
