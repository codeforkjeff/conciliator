package com.codefork.refine.parsers.xml;

import com.codefork.refine.parsers.ParseResult;
import com.codefork.refine.resources.Result;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * SAX parser handler. We use SAX b/c it's faster than loading a whole DOM.
 */
public abstract class XMLParser<R extends ParseResult> extends DefaultHandler {

    protected Map<String, StartElementHandler<R>> startElementHandlers = new HashMap<String, StartElementHandler<R>>();
    protected Map<String, EndElementHandler<R>> endElementHandlers = new HashMap<String, EndElementHandler<R>>();

    public final Stack<String> path = new Stack<String>();

    protected R parseResult;

    public XMLParser() {
        createParseResult();
    }

    public void createParseResult() {
        parseResult = (R) new ParseResult();
    }

    public List<Result> getResults() {
        return parseResult.results;
    }

    public String getPath() {
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
            handler.handle(parseResult, uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        String path = getPath();
        //System.out.println(path);

        EndElementHandler handler = endElementHandlers.get(path);
        if(handler != null) {
            handler.handle(parseResult, uri, localName, qName);
        }

        this.path.pop();
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if(parseResult.captureChars) {
            parseResult.buf.append(ch, start, length);
        }
    }

}
