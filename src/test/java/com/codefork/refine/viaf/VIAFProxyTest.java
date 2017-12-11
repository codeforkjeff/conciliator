package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.HttpServletRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@WebMvcTest(VIAFProxy.class)
public class VIAFProxyTest {

    @TestConfiguration
    static class TestConfig {

        // we can't use MockBean b/c the PostConstruct hook in VIAF uses config
        // before we get a chance to put matchers on it in this test code.
        @Bean
        public Config config() {
            Config config = mock(Config.class);
            when(config.getDataSourceProperties("viafproxy")).thenReturn(new Properties());
            return config;
        }

        @Bean
        public ThreadPoolFactory threadPoolFactory() {
            return new ThreadPoolFactory();
        }
    }

    @MockBean
    ConnectionFactory connectionFactory;

    @Autowired
    VIAFProxy viafProxy;

    @Autowired
    MockMvc mvc;

    @Test
    public void testProxyMetaData() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/viafproxy/LC")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("LC (by way of VIAF)", root.get("name").asText());
        assertEquals("http://id.loc.gov/authorities/names/{{id}}", root.get("view").get("url").asText());
    }

    @Test
    public void testSearchProxyModeLC() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/LC").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("n78095332", result1.get("id").asText());
        assertTrue(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Zamenhof, L. L. (Ludwik Lazar), 1859-1917", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("no90015706", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("n78096841", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * VIAF gives a URL for the BNF source record ID. this test
     * checks that we use the ID parsed out of the "sid" XML element instead.
     */
    @Test
    public void testSearchProxyModeBNF() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/BNF").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("11924607", result1.get("id").asText());
        assertTrue(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Zamenhof, Lejzer Ludwik, 1859-1917", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("12115775", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("11926644", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * Test case for XML missing an ID in ../mainHeadings/data/sources
     * but having an ID under ../VIAFCluster/sources.
     */
    @Test
    public void testSearchProxyModeBNFMissingID() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/alexandre.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"Jean-François Alexandre 1804 1874\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/BNF").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Arago, Jacques, 1790-1855", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("12265696", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Blanc, François 166.-1742", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("10343440", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        // this entry in XML is missing an ID ../mainHeadings/data/sources
        JsonNode result3 = results.get(2);
        assertEquals("Alexandre, Jean-François 1804-1874", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("10341017", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * Test case for URL in source ID mapping, which formatResult() should treat
     * as special case
     */
    @Test
    public void testSearchProxyModeDNB() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/hegel.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"hegel\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/DNB").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Hegel, Georg Wilhelm Friedrich, 1770-1831", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("118547739", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Friedrich, Carl J. 1901-1984", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("118535870", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Bosanquet, Bernard, 1848-1923", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("118659391", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

}
