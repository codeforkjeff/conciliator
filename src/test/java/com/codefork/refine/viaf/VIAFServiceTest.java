package com.codefork.refine.viaf;

import com.codefork.refine.NameType;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.StringUtil;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VIAFServiceTest {

    /**
     * Test that the service is live and that our URL and query params aren't rejected.
     * This doesn't test the XML result we get from VIAF.
     */
    @Test
    public void testDoSearch() throws Exception {
        VIAFService viafService = new VIAFService();

        SearchQuery query = new SearchQuery("william shakespeare", 3, NameType.Person, "should");

        HttpURLConnection conn = viafService.doSearch(query.createCqlQueryString(), query.getLimit());

        // make sure we get something back
        assertNotNull(conn);

        InputStream is = conn.getInputStream();

        assertNotNull(is);

        // as of 5/2/2016, size of XML document returned is 877914.
        // just test that it's > 800k chars, which tells us that it's not an error message
        String s = StringUtil.inputStreamToString(is, 1000000);
        assertTrue(s.length() > 800000);
    }

}