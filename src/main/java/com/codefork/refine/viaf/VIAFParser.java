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

    private List<VIAFResult> results = new ArrayList<VIAFResult>();
    private boolean captureChars = false;
    private boolean insideHeadings = false;
    private boolean insideSources = false;

    // viaf's weird indexed namespacing
    private int nsIndex = 2;

    /** buffer for collecting contents of an Element as parser does processing */
    StringBuilder buf = new StringBuilder();

    public List<VIAFResult> getResults() {
        return results;
    }

    public VIAFResult getLastResult() {
        return results.get(results.size() - 1);
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
        if("record".equals(qName)) {
            results.add(new VIAFResult());
        } else if (getElementNameWithNS("viafID").equals(qName)) {
            captureChars = true;
        } else if (getElementNameWithNS("nameType").equals(qName)) {
            captureChars = true;
        } else if (getElementNameWithNS("mainHeadings").equals(qName)) {
            insideHeadings = true;
        } else if (insideHeadings && getElementNameWithNS("data").equals(qName)) {
            getLastResult().addNameEntry();
        } else if (insideHeadings && getElementNameWithNS("text").equals(qName)) {
            captureChars = true;
        } else if (insideHeadings && getElementNameWithNS("sources").equals(qName)) {
            insideSources = true;
        } else if (insideSources && getElementNameWithNS("s").equals(qName)) {
            captureChars = true;
        } else if (insideHeadings) {
            // if we got here, we encountered some other child of mainHeadings
            // so we want to effectively end the section, otherwise we'll end up
            // erroneously picking up other "text" and "sources" elements nested
            // under other elements in mainHeadings
            insideHeadings = false;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if("record".equals(qName)) {
            nsIndex++;
        } else if (getElementNameWithNS("viafID").equals(qName)) {
            getLastResult().setViafId(buf.toString());
            buf = new StringBuilder();
            captureChars = false;
        } else if (getElementNameWithNS("nameType").equals(qName)) {
            getLastResult().setNameType(NameType.getByViafCode(buf.toString()));
            buf = new StringBuilder();
            captureChars = false;
        } else if (getElementNameWithNS("mainHeadings").equals(qName)) {
            insideHeadings = false;
        } else if (insideHeadings && getElementNameWithNS("text").equals(qName)) {
            getLastResult().getLastNameEntry().setName(buf.toString());
            buf = new StringBuilder();
            captureChars = false;
        } else if (insideHeadings && getElementNameWithNS("sources").equals(qName)) {
            insideSources = false;
        } else if (insideSources && getElementNameWithNS("s").equals(qName)) {
            getLastResult().getLastNameEntry().addSource(buf.toString());
            buf = new StringBuilder();
            captureChars = false;
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if(captureChars) {
            buf.append(ch, start, length);
        }
    }
}
