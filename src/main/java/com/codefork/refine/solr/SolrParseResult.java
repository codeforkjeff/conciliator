package com.codefork.refine.solr;

import com.codefork.refine.parsers.ParseResult;

public class SolrParseResult extends ParseResult {

    enum Field { ID, NAME }

    Field fieldBeingCaptured;
}
