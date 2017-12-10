package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.Result;
import org.junit.After;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VIAFProxyTest {

    ConnectionFactory connectionFactory;
    Config config;
    VIAFProxy viaf;

    @Test
    public void testProxyMetaData() {
        Config config = new Config();
        VIAFProxy viaf = new VIAFProxy();
        viaf.setConfig(config);
        viaf.init();

        VIAFProxyModeMetaDataResponse response = viaf.proxyModeServiceMetaData("LC");
        assertEquals(response.getName(), "LC (by way of VIAF)");
        assertEquals(response.getView().getUrl(), "http://id.loc.gov/authorities/names/{{id}}");
    }

    @Test
    public void testSearchProxyModeLC() throws Exception {
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viafproxy")).thenReturn(new Properties());

        viaf = new VIAFProxy();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, VIAFNameType.Person.asNameType(), "should");
        query.setViafSource("LC");
        query.setViafProxyMode(true);

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
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viafproxy")).thenReturn(new Properties());

        viaf = new VIAFProxy();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, VIAFNameType.Person.asNameType(), "should");
        query.setViafSource("BNF");
        query.setViafProxyMode(true);

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
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/alexandre.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viafproxy")).thenReturn(new Properties());

        viaf = new VIAFProxy();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

        SearchQuery query = new SearchQuery("Jean-François Alexandre 1804 1874", 3, VIAFNameType.Person.asNameType(), "should");
        query.setViafSource("BNF");
        query.setViafProxyMode(true);

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
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/hegel.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        config = mock(Config.class);
        when(config.getDataSourceProperties("viafproxy")).thenReturn(new Properties());

        viaf = new VIAFProxy();
        viaf.setConfig(config);
        viaf.init();
        viaf.setConnectionFactory(connectionFactory);

        SearchQuery query = new SearchQuery("hegel", 3, VIAFNameType.Person.asNameType(), "should");
        query.setViafSource("DNB");
        query.setViafProxyMode(true);

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
