package com.codefork.refine.parsers.xml;

import com.codefork.refine.parsers.ParseResult;
import org.xml.sax.Attributes;

public abstract class StartElementHandler<R extends ParseResult> {
    public abstract void handle(R parseResult, String uri, String localName, String qName, Attributes attributes);
}
