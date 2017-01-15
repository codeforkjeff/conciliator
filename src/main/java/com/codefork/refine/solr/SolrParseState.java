package com.codefork.refine.solr;

import com.codefork.refine.parsers.ParseState;
import com.codefork.refine.resources.NameType;

import java.util.ArrayList;
import java.util.List;

public class SolrParseState extends ParseState {

    enum Field { ID, NAME }

    Field fieldBeingCaptured;

    // we don't yet support multiple name types for Solr records
    // so this is the list we use for every result.
    List<NameType> nameTypes = new ArrayList<NameType>();

}
