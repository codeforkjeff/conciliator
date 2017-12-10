package com.codefork.refine.orcid;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.viaf.VIAF;
import com.codefork.refine.viaf.VIAFNameType;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrcidTest {

    @Test
    public void testOrcidServiceMetaData() {
        Config config = new Config();
        Orcid orcid = new Orcid();
        orcid.setConfig(config);
        orcid.init();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test"));

        ServiceMetaDataResponse response = orcid.serviceMetaData(request);
        assertEquals(response.getName(), "ORCID");
    }

    @Test
    public void testSmartNamesServiceMetaData() {
        Config config = new Config();
        Orcid orcid = new Orcid();
        orcid.setConfig(config);
        orcid.init();

        ServiceMetaDataResponse response = orcid.smartNamesServiceMetaData();
        assertEquals(response.getName(), "ORCID - Smart Names Mode");
    }

    @Test
    public void testLiveSearch() throws Exception {
        Config config = mock(Config.class);
        when(config.getDataSourceProperties("orcid")).thenReturn(new Properties());

        Orcid orcid = new Orcid();
        orcid.setConfig(config);
        orcid.setConnectionFactory(new ConnectionFactory());
            orcid.init();

        String json = "{\"q0\":{\"query\": \"stephen hawking\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        Map<String, SearchResponse> response = orcid.queryMultiple(json);

        assertEquals(3, response.get("q0").getResult().size());

        List<Result> results = response.get("q0").getResult();

        Result result1 = results.get(0);
        assertEquals("Stephen Hawking", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("0000-0002-9079-593X", result1.getId());
        assertFalse(result1.isMatch());
    }

    // https://github.com/codeforkjeff/conciliator/issues/8
    @Test
    public void testLiveSearchSmartNames() throws Exception {
        Config config = mock(Config.class);
        when(config.getDataSourceProperties("orcid")).thenReturn(new Properties());

        Orcid orcid = new Orcid();
        orcid.setConfig(config);
        orcid.setConnectionFactory(new ConnectionFactory());
        orcid.init();

        String json = "{\"q0\":{\"query\": \"Igor Ozerov\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        Map<String, SearchResponse> response = orcid.smartNamesQueryMultiple(json);

        assertEquals(1, response.get("q0").getResult().size());

        List<Result> results = response.get("q0").getResult();

        Result result1 = results.get(0);
        assertEquals("Igor OZEROV", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("0000-0001-5839-7854", result1.getId());
        assertFalse(result1.isMatch());
    }

    @Test
    public void testParseName() throws Exception {
        assertArrayEquals(Orcid.parseName("joe schmoe"),
                new String[] { "joe", "schmoe" });
        assertArrayEquals(Orcid.parseName("schmoe, joe"),
                new String[] { "joe", "schmoe" });
        assertArrayEquals(Orcid.parseName("dr. joe schmoe"),
                null);
    }
}
