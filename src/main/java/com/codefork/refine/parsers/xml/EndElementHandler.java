package com.codefork.refine.parsers.xml;

import com.codefork.refine.parsers.ParseResult;

public abstract class EndElementHandler<R extends ParseResult> {
    public abstract void handle(R parseResult, String uri, String localName, String qName);
}
