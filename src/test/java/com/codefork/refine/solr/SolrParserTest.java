package com.codefork.refine.solr;

import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import org.junit.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SolrParserTest {

    @Test
    public void testParse() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        SolrParser solrParser = new SolrParser("id", "title_display", new NameType("/book/book", "Book"));

        InputStream is = getClass().getResourceAsStream("/solr_results.xml");
        parser.parse(is, solrParser);

        List<Result> results = solrParser.getResults();
        assertEquals(3, results.size());

        Result result1 = results.get(0);
        assertEquals("119390", result1.getId());
        assertEquals("The complete Sherlock Holmes", result1.getName());
        assertEquals("0.33383894", String.valueOf(result1.getScore()));

        Result result2 = results.get(1);
        assertEquals("274753", result2.getId());
        assertEquals("The adventures of Sherlock Holmes", result2.getName());
        assertEquals("0.26951128", String.valueOf(result2.getScore()));

        Result result3 = results.get(2);
        assertEquals("25950", result3.getId());
        assertEquals("The adventures of Sherlock Holmes", result3.getName());
        assertEquals("0.2694855", String.valueOf(result3.getScore()));
    }
}
