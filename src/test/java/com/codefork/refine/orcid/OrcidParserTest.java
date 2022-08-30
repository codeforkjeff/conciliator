package com.codefork.refine.orcid;

import com.codefork.refine.resources.Result;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;



public class OrcidParserTest {

    @Test
    public void testParseNames() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        OrcidParser orcidParser = new OrcidParser();

        InputStream is = getClass().getResourceAsStream("/orcid_results.xml");
        parser.parse(is, orcidParser);

        List<Result> results = orcidParser.getResults();
        assertEquals(10, results.size());

        Result result1 = results.get(0);
        assertEquals("0000-0002-3756-9095", result1.getId());
        assertEquals("jose manuel de la cruz coba", result1.getName());
        assertEquals("0.83170503", String.valueOf(result1.getScore()));

        Result result2 = results.get(1);
        assertEquals("0000-0001-7051-649X", result2.getId());
        assertEquals("Guillermo Manuel Márquez Cruz", result2.getName());
        assertEquals("0.7087979", String.valueOf(result2.getScore()));

        Result result3 = results.get(2);
        assertEquals("0000-0001-6192-4803", result3.getId());
        assertEquals("JESUS MANUEL DE LA CRUZ GARCIA", result3.getName());
        assertEquals("0.66536397", String.valueOf(result3.getScore()));

    }
}
