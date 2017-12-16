package com.codefork.refine.solr;

import com.codefork.refine.parsers.xml.EndElementHandler;
import com.codefork.refine.parsers.xml.StartElementHandler;
import com.codefork.refine.parsers.xml.XMLParser;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import org.xml.sax.Attributes;

public class SolrParser extends XMLParser<SolrParseState> {

    private String fieldId;
    private String fieldName;
    private MultiValueFieldStrategy multiValueFieldStrategy;
    private String multiValueFieldDelimiter;

    /**
     * @param fieldId solr fieldname to use for 'id' field in reconciliation result
     * @param fieldName solr fieldname to use for 'name' field in reconciliation result
     * @param nameType all records parsed from Solr will have this nameType
     */
    public SolrParser(String fieldId, String fieldName, final MultiValueFieldStrategy multiValueFieldStrategy, String multiValueFieldDelimiter, NameType nameType) {
        super();
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.multiValueFieldStrategy = multiValueFieldStrategy;
        this.multiValueFieldDelimiter = multiValueFieldDelimiter;
        this.getParseState().nameTypes.add(nameType);

        startElementHandlers.put("response/result/doc/arr",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        if (SolrParser.this.fieldId.equals(attributes.getValue("name"))) {
                            parseState.fieldBeingCaptured = SolrParseState.Field.ID;
                        } else if (SolrParser.this.fieldName.equals(attributes.getValue("name"))) {
                            parseState.fieldBeingCaptured = SolrParseState.Field.NAME;
                        }
                    }
        });

        startElementHandlers.put("response/result/doc/arr/str",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        if(parseState.fieldBeingCaptured != null) {
                            parseState.captureChars = true;
                        }
                    }
        });

        startElementHandlers.put("response/result/doc/str",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        if (SolrParser.this.fieldId.equals(attributes.getValue("name"))) {
                            parseState.fieldBeingCaptured = SolrParseState.Field.ID;
                        } else if(SolrParser.this.fieldName.equals(attributes.getValue("name"))) {
                            parseState.fieldBeingCaptured = SolrParseState.Field.NAME;
                        }
                        if(parseState.fieldBeingCaptured != null) {
                            parseState.captureChars = true;
                        }
                    }
                });

        startElementHandlers.put("response/result/doc/float",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        if ("score".equals(attributes.getValue("name"))) {
                            parseState.captureChars = true;
                        }
                    }
                });

        startElementHandlers.put("response/result/doc",
                new StartElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName, Attributes attributes) {
                        parseState.result = new Result();
                        parseState.result.setType(parseState.nameTypes);
                    }
                });

        endElementHandlers.put("response/result/doc/arr",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName) {
                        if (SolrParseState.Field.NAME.equals(parseState.fieldBeingCaptured)) {
                            if(MultiValueFieldStrategy.CONCAT.equals(SolrParser.this.multiValueFieldStrategy)) {
                                StringBuilder buf = new StringBuilder();
                                String delim = "";
                                for(String s: parseState.multipleValues) {
                                    buf.append(delim);
                                    buf.append(s);
                                    delim = SolrParser.this.multiValueFieldDelimiter;
                                }
                                parseState.result.setName(buf.toString());
                            } else if(MultiValueFieldStrategy.FIRST.equals(SolrParser.this.multiValueFieldStrategy)) {
                                if(parseState.multipleValues.size() > 0) {
                                    parseState.result.setName(parseState.multipleValues.get(0));
                                }
                            }
                        }
                        parseState.multipleValues.clear();
                        parseState.fieldBeingCaptured = null;
                    }
                });

        endElementHandlers.put("response/result/doc/arr/str",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName) {
                        String s = parseState.buf.toString();
                        if (SolrParseState.Field.NAME.equals(parseState.fieldBeingCaptured)) {
                            parseState.multipleValues.add(s);
                        }
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });

        endElementHandlers.put("response/result/doc/str",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName) {
                        String s = parseState.buf.toString();
                        if (SolrParseState.Field.ID.equals(parseState.fieldBeingCaptured)) {
                            parseState.result.setId(s);
                        } else if (SolrParseState.Field.NAME.equals(parseState.fieldBeingCaptured)) {
                            parseState.result.setName(s);
                        }
                        parseState.fieldBeingCaptured = null;
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });

        endElementHandlers.put("response/result/doc/float",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName) {
                        String s = parseState.buf.toString();
                        parseState.result.setScore(Double.valueOf(s));
                        parseState.buf = new StringBuilder();
                        parseState.captureChars = false;
                    }
                });

        endElementHandlers.put("response/result/doc",
                new EndElementHandler<SolrParseState>() {
                    public void handle(SolrParseState parseState, String uri, String localName, String qName) {
                        parseState.results.add(parseState.result);
                        parseState.result = null;
                    }
                });

    }

    @Override
    public SolrParseState createParseState() {
        return new SolrParseState();
    }

}
