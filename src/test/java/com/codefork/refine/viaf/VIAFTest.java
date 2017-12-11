package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@WebMvcTest(VIAF.class)
public class VIAFTest {

    @TestConfiguration
    static class TestConfig {

        // we can't use MockBean b/c the PostConstruct hook in VIAF uses config
        // before we get a chance to put matchers on it in this test code.
        @Bean
        public Config config() {
            Config config = mock(Config.class);
            when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());
            return config;
        }
    }

    @MockBean
    ConnectionFactory connectionFactory;

    @Autowired
    VIAF viaf;

    @Autowired
    MockMvc mvc;

    @Test
    public void testServiceMetadata() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/viaf")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("VIAF", root.get("name").asText());
        assertEquals("http://viaf.org/viaf/{{id}}", root.get("view").get("url").asText());
    }

    @Test
    public void testReconcileRequest() throws Exception {
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

        String json = "{\"q0\":{\"query\": \"shakespeare\",\"type\":\"/people/person\",\"type_strict\":\"should\"},\"q1\":{\"query\":\"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals(2, root.size());

        assertEquals(3, root.get("q0").get("result").size());
        JsonNode result = root.get("q0").get("result").get(0);
        assertEquals("96994048", result.get("id").asText());
        assertEquals("Shakespeare, William, 1564-1616.", result.get("name").asText());
        assertEquals("/people/person", result.get("type").get(0).get("id").asText());
        assertEquals("Person", result.get("type").get(0).get("name").asText());
        assertEquals("0.3125", result.get("score").asText());
        assertFalse(result.get("match").asBoolean());

        assertEquals(3, root.get("q1").get("result").size());
        JsonNode result2 = root.get("q1").get("result").get(0);

        assertEquals("24609378", result2.get("id").asText());
        assertEquals("Wittgenstein, Ludwig, 1889-1951", result2.get("name").asText());
        assertEquals("/people/person", result2.get("type").get(0).get("id").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("0.3548387096774194", result2.get("score").asText());
        assertFalse(result2.get("match").asBoolean());
    }

    public void testSearchSingle(String queryValue) throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/wittgenstein.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("query", queryValue)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Wittgenstein, Ludwig, 1889-1951", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("24609378", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Anscombe, G. E. M. (Gertrude Elizabeth Margaret)", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("59078032", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Klossowski, Pierre, 1905-2001", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("27066848", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchSingleWithText() throws Exception {
        testSearchSingle("wittgenstein");
    }

    @Test
    public void testSearchSingleWithJson() throws Exception {
        String json = "{\"query\": \"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}";
        testSearchSingle(json);
    }

    @Test
    public void testSearchPersonalName() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/wittgenstein.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Wittgenstein, Ludwig, 1889-1951", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("24609378", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Anscombe, G. E. M. (Gertrude Elizabeth Margaret)", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("59078032", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Klossowski, Pierre, 1905-2001", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("27066848", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchNoParticularType() throws Exception {
        // This is when you get when you choose "Reconcile against no particular type" in OpenRefine

        // http://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22John%20Steinbeck%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/steinbeck_no_type.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"steinbeck\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Steinbeck, John, 1902-1968", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("96992551", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Steinbeck, John 1946-1991", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("19893647", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Steinbeck, John, 1902-1968. | Of mice and men.", result3.get("name").asText());
        assertEquals("Work", result3.get("type").get(0).get("name").asText());
        assertEquals("180993990", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchWithSource() throws Exception {
        // Also chose "Reconcile against no particular type" for this one

        // https://viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22Vladimir%20Nabokov%22%20and%20local.sources%20%3D%20%22nsk%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/nabokov_nsk.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"nabokov\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf/NSK").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Nabokov, Vladimir Vladimirovič", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("27069388", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Nabokov, Vladimir Vladimirovič | Lolita", result2.get("name").asText());
        assertEquals("Work", result2.get("type").get(0).get("name").asText());
        assertEquals("176671347", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Nabokov, Vladimir Vladimirovič | Govori, sjećanje!", result3.get("name").asText());
        assertEquals("Work", result3.get("type").get(0).get("name").asText());
        assertEquals("183561595", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchWithExactMatch() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("96994048", result1.get("id").asText());
        assertTrue(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Zamenhof, L. L. (Ludwik Lazar), 1859-1917", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("73885295", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("34463780", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchWithNoResults() throws Exception {
        // https://viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22ncjecerence%22%20and%20local.sources%20%3D%20%22nsk%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/nonsense.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"ncjecerence\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(0, results.size());
    }

    @Test
    public void testCache() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        viaf.setCacheLifetime(1);

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        JsonNode results = new ObjectMapper()
                .readTree(mvcResult.getResponse().getContentAsString())
                .get("q0").get("result");
        assertEquals(3, results.size());

        mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();
        assertEquals(3, results.size());

        JsonNode results2 = new ObjectMapper()
                .readTree(mvcResult.getResponse().getContentAsString())
                .get("q0").get("result");
        assertEquals(3, results2.size());

        verify(connectionFactory, times(1)).createConnection(anyString());
    }

    @Test
    public void testExpireCache() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        final Class testClass = getClass();
        doAnswer(new Answer<HttpURLConnection>() {
            @Override
            public HttpURLConnection answer(InvocationOnMock invocation) throws Exception {
                HttpURLConnection conn = mock(HttpURLConnection.class);
                when(conn.getInputStream()).thenReturn(testClass.getResourceAsStream("/shakespeare.xml"));
                return conn;
            }
        }).when(connectionFactory).createConnection(anyString());

        viaf.setCacheLifetime(1);

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        JsonNode results = new ObjectMapper()
                .readTree(mvcResult.getResponse().getContentAsString())
                .get("q0").get("result");
        assertEquals(3, results.size());

        Thread.sleep(1100);
        viaf.expireCache();

        mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();
        assertEquals(3, results.size());

        JsonNode results2 = new ObjectMapper()
                .readTree(mvcResult.getResponse().getContentAsString())
                .get("q0").get("result");
        assertEquals(3, results2.size());

        verify(connectionFactory, times(2)).createConnection(anyString());
    }

    @After
    public void cleanup() throws Exception {
        viaf.expireCache();
        // sleep to give the cache a chance to expire
        Thread.sleep(1000);
    }
}
