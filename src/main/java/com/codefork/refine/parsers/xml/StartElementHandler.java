package com.codefork.refine.parsers.xml;

import com.codefork.refine.parsers.ParseState;
import org.xml.sax.Attributes;

public abstract class StartElementHandler<R extends ParseState> {
    public abstract void handle(R parseResult, String uri, String localName, String qName, Attributes attributes);
}
