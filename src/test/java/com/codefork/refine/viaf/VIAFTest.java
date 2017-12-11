package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.NameType;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: break this up into VIAFLiveTest and VIAFNonLiveTest
// (some of this code has been moved already)
public class VIAFTest {

    ConnectionFactory connectionFactory;
    Config config;
    VIAF viaf;

    @Test
    public void testServiceMetadata() throws Exception {
        Config config = new Config();
        VIAF viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test"));

        ServiceMetaDataResponse response = viaf.serviceMetaData(request);
        assertEquals(response.getName(), "VIAF");
        assertEquals(response.getView().getUrl(), "http://viaf.org/viaf/{{id}}");
    }

    /**
     * Simple test for parsing live VIAF XML
     */
    @Test
    public void testLiveSearch() throws Exception {
        config = mock(Config.class);
        when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.setConfig(config);
        viaf.setConnectionFactory(new ConnectionFactory());
        viaf.init();

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
    public void testReconcileRequest() throws Exception {
        Config config = new Config();

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        final Class testClass = getClass();
        doAnswer(new Answer<HttpURLConnection>() {
            @Override
            public HttpURLConnection answer(InvocationOnMock invocation) throws Exception {
                String arg1 = (String) invocation.getArguments()[0];
                if (arg1.contains("shakespeare")) {
                    HttpURLConnection conn = mock(HttpURLConnection.class);
                    when(conn.getInputStream()).thenReturn(testClass.getResourceAsStream("/shakespeare.xml"));
                    return conn;
                } else if (arg1.contains("wittgenstein")) {
                    HttpURLConnection conn = mock(HttpURLConnection.class);
                    when(conn.getInputStream()).thenReturn(testClass.getResourceAsStream("/wittgenstein.xml"));
                    return conn;
                }
                return null;
            }
        }).when(connectionFactory).createConnection(anyString());

        VIAF viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

        String json = "{\"q0\":{\"query\": \"shakespeare\",\"type\":\"/people/person\",\"type_strict\":\"should\"},\"q1\":{\"query\":\"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        Map<String, SearchResponse> results = viaf.queryMultiple(json);

        assertEquals(results.size(), 2);

        SearchResponse response = results.get("q0");
        List<Result> result = response.getResult();
        assertEquals(result.size(), 3);
        assertEquals(result.get(0).getId(), "96994048");
        assertEquals(result.get(0).getName(), "Shakespeare, William, 1564-1616.");
        assertEquals(result.get(0).getType().get(0).getId(), "/people/person");
        assertEquals(result.get(0).getType().get(0).getName(), "Person");
        assertEquals(String.valueOf(result.get(0).getScore()), "0.3125");
        assertEquals(result.get(0).isMatch(), false);

        SearchResponse response2 = results.get("q1");
        List<Result> result2 = response2.getResult();
        assertEquals(result2.size(), 3);
        assertEquals(result2.get(0).getId(), "24609378");
        assertEquals(result2.get(0).getName(), "Wittgenstein, Ludwig, 1889-1951");
        assertEquals(result2.get(0).getType().get(0).getId(), "/people/person");
        assertEquals(result2.get(0).getType().get(0).getName(), "Person");
        assertEquals(String.valueOf(result2.get(0).getScore()), "0.3548387096774194");
        assertEquals(result2.get(0).isMatch(), false);
    }

    @Test
    public void testSearchPersonalName() throws Exception {
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/wittgenstein.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

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
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/steinbeck_no_type.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

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
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/nabokov_nsk.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

        SearchQuery query = new SearchQuery("nabokov", 3, null, "should");
        query.setViafSource("NSK");
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
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

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
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/nonsense.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

        SearchQuery query = new SearchQuery("ncjecerence", 3, null, "should");
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(0, results.size());
    }

    @Test
    public void testCache() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);
        viaf.setCacheEnabled(true);
        viaf.setCacheLifetime(1);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, new NameType(VIAFNameType.Person.getId(), null), "should");
        List<Result> results = viaf.searchCheckCache(query);

        assertEquals(3, results.size());

        SearchQuery query2 = new SearchQuery("Shakespeare, William, 1564-1616.", 3, new NameType(VIAFNameType.Person.getId(), null), "should");
        List<Result> results2 = viaf.searchCheckCache(query2);

        assertEquals(3, results2.size());

        verify(connectionFactory, times(1)).createConnection(anyString());
    }

    @Test
    public void testExpireCache() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        connectionFactory = mock(ConnectionFactory.class);
        final Class testClass = getClass();
        doAnswer(new Answer<HttpURLConnection>() {
            @Override
            public HttpURLConnection answer(InvocationOnMock invocation) throws Exception {
                HttpURLConnection conn = mock(HttpURLConnection.class);
                when(conn.getInputStream()).thenReturn(testClass.getResourceAsStream("/shakespeare.xml"));
                return conn;
            }
        }).when(connectionFactory).createConnection(anyString());

        config = mock(Config.class);
        when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());

        viaf = new VIAF();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);
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

        verify(connectionFactory, times(2)).createConnection(anyString());
    }

    @After
    public void shutdownVIAF() {
        if(viaf != null) {
            viaf.shutdown();
        }
    }

}