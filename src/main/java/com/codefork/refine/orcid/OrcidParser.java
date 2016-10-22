package com.codefork.refine.orcid;

import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
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
public class OrcidParser extends DefaultHandler {

    private static abstract class StartElementHandler {
        public abstract void handle(OrcidParser parser, String uri, String localName, String qName, Attributes attributes);
    }

    private static abstract class EndElementHandler {
        public abstract void handle(OrcidParser parser, String uri, String localName, String qName);
    }

    private static final Map<String, StartElementHandler> startElementHandlers = new HashMap<String, StartElementHandler>();
    private static final Map<String, EndElementHandler> endElementHandlers = new HashMap<String, EndElementHandler>();

    static {
        final List<NameType> nameTypes = new ArrayList<NameType>();
        nameTypes.add(new NameType("/people/person", "Person"));

        startElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result",
                new StartElementHandler() {
                    public void handle(OrcidParser parser, String uri, String localName, String qName, Attributes attributes) {
                        parser.result = new Result();
                        parser.result.setType(nameTypes);
                    }
                });

        StartElementHandler captureHandler = new StartElementHandler() {
            public void handle(OrcidParser parser, String uri, String localName, String qName, Attributes attributes) {
                parser.captureChars = true;
            }
        };

        startElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/relevancy-score",
                captureHandler);
        startElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-identifier/path",
                captureHandler);
        startElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/given-names",
                captureHandler);
        startElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/family-name",
                captureHandler);

        endElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result",
                new EndElementHandler() {
                    public void handle(OrcidParser parser, String uri, String localName, String qName) {
                        parser.results.add(parser.result);
                        parser.result = null;
                    }
                });
        endElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/relevancy-score",
                new EndElementHandler() {
                    public void handle(OrcidParser parser, String uri, String localName, String qName) {
                        parser.result.setScore(Double.valueOf(parser.buf.toString()));
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });
        endElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-identifier/path",
                new EndElementHandler() {
                    public void handle(OrcidParser parser, String uri, String localName, String qName) {
                        parser.result.setId(parser.buf.toString());
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });
        endElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/given-names",
                new EndElementHandler() {
                    public void handle(OrcidParser parser, String uri, String localName, String qName) {
                        parser.result.setName(parser.buf.toString());
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });
        endElementHandlers.put("orcid-message/orcid-search-results/orcid-search-result/orcid-profile/orcid-bio/personal-details/family-name",
                new EndElementHandler() {
                    public void handle(OrcidParser parser, String uri, String localName, String qName) {
                        String nameSoFar = "";
                        if(parser.result.getName() != null) {
                            nameSoFar = parser.result.getName();
                        }
                        String sep = "";
                        if(nameSoFar.length() > 0) {
                            sep = " ";
                        }
                        parser.result.setName(nameSoFar + sep + parser.buf.toString());
                        parser.buf = new StringBuilder();
                        parser.captureChars = false;
                    }
                });

    }

    private boolean captureChars = false;

    private final Stack<String> path = new Stack<String>();

    private final List<Result> results = new ArrayList<Result>();
    private Result result = null;

    /** buffer for collecting contents of an Element as parser does processing */
    private StringBuilder buf = new StringBuilder();

    public List<Result> getResults() {
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

}
