package com.codefork.refine.parsers;

import com.codefork.refine.resources.Result;

import java.util.ArrayList;
import java.util.List;

public class ParseState {

    public boolean captureChars = false;

    public final List<Result> results = new ArrayList<Result>();
    public Result result = null;

    /** buffer for collecting contents of an Element as parser does processing */
    public StringBuilder buf = new StringBuilder();

}
