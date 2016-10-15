package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VIAFTest {

    VIAFService viafService;
    Config config;
    VIAF viaf;

    /**
     * Simple test for parsing live VIAF XML
     */
    @Test
    public void testLiveSearch() throws Exception {
        viafService = new VIAFService();

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);

        SearchQuery query = new SearchQuery("shakespeare", 3, null, "should");
        List<Result> results = viaf.searchCheckCache(query);

        // first result for shakespeare shouldn't ever change;
        // it makes for a fairly stable test

        assertEquals(3, results.size());

        Result result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("96994048", result1.getId());
        assertFalse(result1.isMatch());
    }

    @Test
    public void testSearchPersonalName() throws Exception {
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/wittgenstein.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        SearchQuery query = new SearchQuery("wittgenstein", 3, VIAFNameType.Person.asNameType(), "should");
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());

        Result result1 = results.get(0);
        assertEquals("Wittgenstein, Ludwig, 1889-1951", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("24609378", result1.getId());
        assertFalse(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Anscombe, G. E. M. (Gertrude Elizabeth Margaret)", result2.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result2.getType().get(0));
        assertEquals("59078032", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Klossowski, Pierre, 1905-2001", result3.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result3.getType().get(0));
        assertEquals("27066848", result3.getId());
        assertFalse(result3.isMatch());
    }

    @Test
    public void testSearchNoParticularType() throws Exception {
        // This is when you get when you choose "Reconcile against no particular type" in OpenRefine

        // http://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22John%20Steinbeck%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/steinbeck_no_type.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        SearchQuery query = new SearchQuery("steinbeck", 3, null, "should");
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Steinbeck, John, 1902-1968", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("96992551", result1.getId());
        assertFalse(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Steinbeck, John 1946-1991", result2.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result2.getType().get(0));
        assertEquals("19893647", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Steinbeck, John, 1902-1968. | Of mice and men.", result3.getName());
        assertEquals(VIAFNameType.Book.asNameType(), result3.getType().get(0));
        assertEquals("180993990", result3.getId());
        assertFalse(result3.isMatch());
    }

    @Test
    public void testSearchWithSource() throws Exception {
        // Also chose "Reconcile against no particular type" for this one

        // https://viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22Vladimir%20Nabokov%22%20and%20local.sources%20%3D%20%22nsk%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/nabokov_nsk.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(VIAF.EXTRA_PARAM_SOURCE_FROM_PATH, "NSK"); // NSK=Croatia

        SearchQuery query = new SearchQuery("nabokov", 3, null, "should", extraParams);
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Nabokov, Vladimir Vladimirovič", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("27069388", result1.getId());
        assertFalse(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Nabokov, Vladimir Vladimirovič | Lolita", result2.getName());
        assertEquals(VIAFNameType.Book.asNameType(), result2.getType().get(0));
        assertEquals("176671347", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Nabokov, Vladimir Vladimirovič | Govori, sjećanje!", result3.getName());
        assertEquals(VIAFNameType.Book.asNameType(), result3.getType().get(0));
        assertEquals("183561595", result3.getId());
        assertFalse(result3.isMatch());
    }

    @Test
    public void testSearchWithExactMatch() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, VIAFNameType.Person.asNameType(), "should");
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("96994048", result1.getId());
        assertTrue(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Zamenhof, L. L. (Ludwik Lazar), 1859-1917", result2.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result2.getType().get(0));
        assertEquals("73885295", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result3.getType().get(0));
        assertEquals("34463780", result3.getId());
        assertFalse(result3.isMatch());
    }

    @Test
    public void testSearchWithNoResults() throws Exception {

        // https://viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22ncjecerence%22%20and%20local.sources%20%3D%20%22nsk%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/nonsense.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        SearchQuery query = new SearchQuery("ncjecerence", 3, null, "should");
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(0, results.size());
    }

    @Test
    public void testCache() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);
        viaf.setCacheEnabled(true);
        viaf.setCacheLifetime(1);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, new NameType(VIAFNameType.Person.getId(), null), "should");
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());

        SearchQuery query2 = new SearchQuery("Shakespeare, William, 1564-1616.", 3, new NameType(VIAFNameType.Person.getId(), null), "should");
        List<Result> results2 = viaf.searchCheckCache(query2);

        assertEquals(3, results2.size());

        verify(viafService, times(1)).doSearch(anyString(), anyInt());
    }

    @Test
    public void testExpireCache() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        final Class testClass = getClass();
        doAnswer(new Answer<HttpURLConnection>() {
            @Override
            public HttpURLConnection answer(InvocationOnMock invocation) throws Exception {
                HttpURLConnection conn = mock(HttpURLConnection.class);
                when(conn.getInputStream()).thenReturn(testClass.getResourceAsStream("/shakespeare.xml"));
                return conn;
            }
        }).when(viafService).doSearch(anyString(), anyInt());

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);
        viaf.setCacheEnabled(true);
        viaf.setCacheLifetime(1);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, VIAFNameType.Person.asNameType(), "should");
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());

        Thread.sleep(1100);
        viaf.expireCache();

        SearchQuery query2 = new SearchQuery("Shakespeare, William, 1564-1616.", 3, VIAFNameType.Person.asNameType(), "should");
        List<Result> results2 = viaf.searchCheckCache(query2);

        assertEquals(3, results2.size());

        verify(viafService, times(2)).doSearch(anyString(), anyInt());
    }

    @Test
    public void testSearchProxyModeLC() throws Exception {
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(VIAF.EXTRA_PARAM_SOURCE_FROM_PATH, "LC");
        extraParams.put(VIAF.EXTRA_PARAM_PROXY_MODE, "true");

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, VIAFNameType.Person.asNameType(), "should", extraParams);

        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("n78095332", result1.getId());
        assertTrue(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Zamenhof, L. L. (Ludwik Lazar), 1859-1917", result2.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result2.getType().get(0));
        assertEquals("no90015706", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result3.getType().get(0));
        assertEquals("n78096841", result3.getId());
        assertFalse(result3.isMatch());
    }

    /**
     * VIAF gives a URL for the BNF source record ID. this test
     * checks that we use the ID parsed out of the "sid" XML element instead.
     */
    @Test
    public void testSearchProxyModeBNF() throws Exception {
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(VIAF.EXTRA_PARAM_SOURCE_FROM_PATH, "BNF");
        extraParams.put(VIAF.EXTRA_PARAM_PROXY_MODE, "true");

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, VIAFNameType.Person.asNameType(), "should", extraParams);

        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("11924607", result1.getId());
        assertTrue(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Zamenhof, Lejzer Ludwik, 1859-1917", result2.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result2.getType().get(0));
        assertEquals("12115775", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result3.getType().get(0));
        assertEquals("11926644", result3.getId());
        assertFalse(result3.isMatch());
    }

    /**
     * Test case for XML missing an ID in ../mainHeadings/data/sources
     * but having an ID under ../VIAFCluster/sources.
     */
    @Test
    public void testSearchProxyModeBNFMissingID() throws Exception {
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/alexandre.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(VIAF.EXTRA_PARAM_SOURCE_FROM_PATH, "BNF");
        extraParams.put(VIAF.EXTRA_PARAM_PROXY_MODE, "true");

        SearchQuery query = new SearchQuery("Jean-François Alexandre 1804 1874", 3, VIAFNameType.Person.asNameType(), "should", extraParams);

        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());

        Result result1 = results.get(0);
        assertEquals("Arago, Jacques, 1790-1855", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("12265696", result1.getId());
        assertFalse(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Blanc, François 166.-1742", result2.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result2.getType().get(0));
        assertEquals("10343440", result2.getId());
        assertFalse(result2.isMatch());

        // this entry in XML is missing an ID ../mainHeadings/data/sources
        Result result3 = results.get(2);
        assertEquals("Alexandre, Jean-François 1804-1874", result3.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result3.getType().get(0));
        assertEquals("10341017", result3.getId());
        assertFalse(result3.isMatch());
    }

    /**
     * Test case for URL in source ID mapping, which formatResult() should treat
     * as special case
     */
    @Test
    public void testSearchProxyModeDNB() throws Exception {
        viafService = mock(VIAFService.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/hegel.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.init(config);
        viaf.setViafService(viafService);

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(VIAF.EXTRA_PARAM_SOURCE_FROM_PATH, "DNB");
        extraParams.put(VIAF.EXTRA_PARAM_PROXY_MODE, "true");

        SearchQuery query = new SearchQuery("hegel", 3, VIAFNameType.Person.asNameType(), "should", extraParams);

        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());

        Result result1 = results.get(0);
        assertEquals("Hegel, Georg Wilhelm Friedrich, 1770-1831", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("118547739", result1.getId());
        assertFalse(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Friedrich, Carl J. 1901-1984", result2.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result2.getType().get(0));
        assertEquals("118535870", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Bosanquet, Bernard, 1848-1923", result3.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result3.getType().get(0));
        assertEquals("118659391", result3.getId());
        assertFalse(result3.isMatch());
    }

    @After
    public void shutdownVIAF() {
        if(viaf != null) {
            viaf.shutdown();
        }
    }

}