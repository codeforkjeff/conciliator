package com.codefork.refine.viaf;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.viaf.sources.VIAFSource;
import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VIAFParserTest {

    private static String joinStrings(List<String> strings, String delimiter) {
        StringBuilder b = new StringBuilder();
        for(String s : strings) {
            if(b.length() > 0) {
                b.append(delimiter);
            }
            b.append(s);
        }
        return b.toString();
    }

    private static String joinSources(List<NameSource> nameSources, String delimiter) {
        StringBuilder b = new StringBuilder();
        for(NameSource s : nameSources) {
            if(b.length() > 0) {
                b.append(delimiter);
            }
            b.append(s.getSource());
        }
        return b.toString();
    }

    /**
     * calculate arithmetic mean (average)
     * @param ary
     * @return
     */
    private static long mean(long[] ary) {
        long avg = 0;
        int t = 1;
        for (long x : ary) {
            avg += (x - avg) / t;
            ++t;
        }
        return avg;
    }

    private void benchmarkParser(Class parserClass, int n) throws Exception {
        long times[] = new long[n];
        for(int i = 0; i < n; i++) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser parser = spf.newSAXParser();
            DefaultHandler viafParser = (DefaultHandler) parserClass.newInstance();

            InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
            long start = System.currentTimeMillis();
            parser.parse(is, viafParser);
            long end = System.currentTimeMillis();

            times[i] = end - start;
        }
        System.out.println(String.format("parse using %s, mean time over %s runs=%s", parserClass.toString(), n, mean(times)));
    }

    /*
    @Test
    public void testAverages() throws Exception {
        benchmarkParser(VIAFParser.class, 100);
    }
    */

    @Test
    public void testParseNames() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        VIAFParser viafParser = new VIAFParser(new VIAFSource(), new SearchQuery("x", 3, new NameType("x", "x"), ""));

        InputStream is = getClass().getResourceAsStream("/steinbeck_no_type.xml");
        parser.parse(is, viafParser);

        List<VIAFResult> results = viafParser.getParseState().getViafResults();

        VIAFResult firstResult = results.get(0);
        VIAFResult secondResult = results.get(1);

        assertEquals(10, firstResult.getNameEntries().size());

        assertEquals("Steinbeck, John, 1902-1968",
                firstResult.getNameEntries().get(0).getName());
        assertEquals("LC,BIBSYS,BNF,KRNLK,N6I,LAC,BNE,SUDOC,BAV,BNC,NLI,B2Q,PTBNP,NLP,LNB,SELIBR,NLA,ICCU,NDL,DNB,NUKAT,NKC",
                joinSources(firstResult.getNameEntries().get(0).getNameSources(), ","));

        // test that our source ID mappings work
        assertEquals("n79081460", firstResult.getSourceNameId("LC"));
        assertEquals("x90081598", firstResult.getSourceNameId("BIBSYS"));
        assertEquals("http://catalogue.bnf.fr/ark:/12148/cb119254833", firstResult.getSourceNameId("BNF"));
        assertEquals("IT\\ICCU\\CFIV\\000628", firstResult.getSourceNameId("ICCU"));

        assertEquals("Steinbeck, John (John Ernst), 1902-1968",
                firstResult.getNameEntries().get(1).getName());
        assertEquals("NTA",
                joinSources(firstResult.getNameEntries().get(1).getNameSources(), ","));

        assertEquals("NSK,SWNL",
                joinSources(firstResult.getNameEntries().get(2).getNameSources(), ","));
        assertEquals("WKP",
                joinSources(firstResult.getNameEntries().get(3).getNameSources(), ","));
        assertEquals("LNL,EGAXA",
                joinSources(firstResult.getNameEntries().get(4).getNameSources(), ","));
        assertEquals("NLI",
                joinSources(firstResult.getNameEntries().get(5).getNameSources(), ","));
        assertEquals("NLI",
                joinSources(firstResult.getNameEntries().get(6).getNameSources(), ","));
        assertEquals("NLI",
                joinSources(firstResult.getNameEntries().get(7).getNameSources(), ","));
        assertEquals("NLR",
                joinSources(firstResult.getNameEntries().get(8).getNameSources(), ","));
        assertEquals("JPG",
                joinSources(firstResult.getNameEntries().get(9).getNameSources(), ","));

        assertEquals(5, secondResult.getNameEntries().size());

        assertEquals("Steinbeck, John 1946-1991",
                secondResult.getNameEntries().get(0).getName());
        assertEquals("NLP,ICCU,DNB,BNF",
                joinSources(secondResult.getNameEntries().get(0).getNameSources(), ","));

    }

    @Test
    public void testParseTime() throws Exception {

        int maxTime = 150; // milliseconds

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();

        int n = 100;
        long start = System.currentTimeMillis();
        for(int i = 0; i < n; i++) {
            VIAFParser viafParser = new VIAFParser(new VIAFSource(), new SearchQuery("x", 3, new NameType("x", "x"), ""));
            InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
            parser.parse(is, viafParser);
        }
        long t = (System.currentTimeMillis() - start) / n;

        // should take less than
        assertTrue(t < maxTime, "should take less than " + maxTime + "ms, on average, to parse a big XML doc, but took " + t + "ms");
    }

}
