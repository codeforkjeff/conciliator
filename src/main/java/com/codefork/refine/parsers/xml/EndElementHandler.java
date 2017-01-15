package com.codefork.refine.parsers.xml;

import com.codefork.refine.parsers.ParseState;

public abstract class EndElementHandler<R extends ParseState> {
    public abstract void handle(R parseResult, String uri, String localName, String qName);
}
