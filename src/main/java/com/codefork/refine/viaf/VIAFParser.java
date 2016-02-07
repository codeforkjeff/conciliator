package com.codefork.refine.viaf;

import com.codefork.refine.NameType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * SAX parser handler. We use SAX b/c it's faster than loading a whole DOM.
 */
public class VIAFParser extends DefaultHandler {

    private boolean captureChars = false;

    private List<VIAFResult> results = new ArrayList<VIAFResult>();
    private VIAFResult result = null;
    private List<NameEntry> nameEntries = null;
    private NameEntry nameEntry = null;
    private List<String> sources = null;

    // viaf's weird indexed namespacing
    private int nsIndex = 2;

    /** buffer for collecting contents of an Element as parser does processing */
    StringBuilder buf = new StringBuilder();

    public List<VIAFResult> getResults() {
        return results;
    }

    /**
     * Elements in the XML have namespaces that are indexed. This returns
     * the namespace prefix for the current namespace index.
     * @param name the elementName to which the namespace is prepended
     * @return fully qualified element name
     */
    public String getElementNameWithNS(String name) {
        return "ns" + nsIndex + ":" + name;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (result != null) {
            if (nameEntries != null) {
                if (nameEntry != null) {
                    if (sources != null) {
                        if (getElementNameWithNS("s").equals(qName)) {
                            captureChars = true;
                        }
                    } else if (getElementNameWithNS("sources").equals(qName)) {
                        sources = new ArrayList<String>();
                    } else if (getElementNameWithNS("text").equals(qName)) {
                        captureChars = true;
                    }
                } else if (getElementNameWithNS("data").equals(qName)) {
                    nameEntry = new NameEntry();
                }
            } else if (getElementNameWithNS("mainHeadings").equals(qName)) {
                nameEntries = new ArrayList<NameEntry>();
            } else if (getElementNameWithNS("viafID").equals(qName)) {
                captureChars = true;
            } else if (getElementNameWithNS("nameType").equals(qName)) {
                captureChars = true;
            }
        } else if("record".equals(qName)) {
            result = new VIAFResult();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (result != null) {
            if("record".equals(qName)) {
                results.add(result);
                result = null;
                nsIndex++;
            } else if (nameEntries != null) {
                if (getElementNameWithNS("mainHeadings").equals(qName)) {
                    result.setNameEntries(nameEntries);
                    nameEntries = null;
                } else if (nameEntry != null) {
                    if (getElementNameWithNS("data").equals(qName)) {
                        nameEntries.add(nameEntry);
                        nameEntry = null;
                    } else if (getElementNameWithNS("text").equals(qName)) {
                        nameEntry.setName(buf.toString());
                        buf = new StringBuilder();
                        captureChars = false;
                    } else if (sources != null) {
                        if (getElementNameWithNS("sources").equals(qName)) {
                            nameEntry.setSources(sources);
                            sources = null;
                        } else if (getElementNameWithNS("s").equals(qName)) {
                            sources.add(buf.toString());
                            buf = new StringBuilder();
                            captureChars = false;
                        }
                    }
                }
            } else if (getElementNameWithNS("viafID").equals(qName)) {
                result.setViafId(buf.toString());
                buf = new StringBuilder();
                captureChars = false;
            } else if (getElementNameWithNS("nameType").equals(qName)) {
                result.setNameType(NameType.getByViafCode(buf.toString())) ;
                buf = new StringBuilder();
                captureChars = false;
            }
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if(captureChars) {
            buf.append(ch, start, length);
        }
    }
}
