package com.codefork.refine.solr;

import com.codefork.refine.parsers.ParseState;

public class SolrParseState extends ParseState {

    enum Field { ID, NAME }

    Field fieldBeingCaptured;
}
