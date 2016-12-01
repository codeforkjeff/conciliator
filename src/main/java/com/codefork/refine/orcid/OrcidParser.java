package com.codefork.refine.orcid;

import com.codefork.refine.parsers.ParseResult;
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
public class OrcidParser extends XMLParser {

    private final static Map<String, StartElementHandler> staticStartElementHandlers = new HashMap<String, StartElementHandler>();
    private final static Map<String, EndElementHandler> staticEndElementHandlers  = new HashMap<String, EndElementHandler>();

    static {
        final List<NameType> nameTypes = new ArrayList<NameType>();
        nameTypes.add(new NameType("/people/person", "Person"));

        staticStartElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result",
                new StartElementHandler() {
                    public void handle(ParseResult parseResult, String uri, String localName, String qName, Attributes attributes) {
                        parseResult.result = new Result();
                        parseResult.result.setType(nameTypes);
                    }
                });

        StartElementHandler captureHandler = new StartElementHandler() {
            public void handle(ParseResult parseResult, String uri, String localName, String qName, Attributes attributes) {
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
                new EndElementHandler() {
                    public void handle(ParseResult parseResult, String uri, String localName, String qName) {
                        parseResult.results.add(parseResult.result);
                        parseResult.result = null;
                    }
                });
        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/relevancy-score",
                new EndElementHandler() {
                    public void handle(ParseResult parseResult, String uri, String localName, String qName) {
                        parseResult.result.setScore(Double.valueOf(parseResult.buf.toString()));
                        parseResult.buf = new StringBuilder();
                        parseResult.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-identifier/path",
                new EndElementHandler() {
                    public void handle(ParseResult parseResult, String uri, String localName, String qName) {
                        parseResult.result.setId(parseResult.buf.toString());
                        parseResult.buf = new StringBuilder();
                        parseResult.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/given-names",
                new EndElementHandler() {
                    public void handle(ParseResult parseResult, String uri, String localName, String qName) {
                        parseResult.result.setName(parseResult.buf.toString());
                        parseResult.buf = new StringBuilder();
                        parseResult.captureChars = false;
                    }
                });
        staticEndElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/family-name",
                new EndElementHandler() {
                    public void handle(ParseResult parseResult, String uri, String localName, String qName) {
                        String nameSoFar = "";
                        if(parseResult.result.getName() != null) {
                            nameSoFar = parseResult.result.getName();
                        }
                        String sep = "";
                        if(nameSoFar.length() > 0) {
                            sep = " ";
                        }
                        parseResult.result.setName(nameSoFar + sep + parseResult.buf.toString());
                        parseResult.buf = new StringBuilder();
                        parseResult.captureChars = false;
                    }
                });

    }

    public OrcidParser() {
        super();
        this.startElementHandlers = staticStartElementHandlers;
        this.endElementHandlers = staticEndElementHandlers;
    }

}
