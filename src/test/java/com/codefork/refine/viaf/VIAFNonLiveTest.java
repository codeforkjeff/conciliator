package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.datasource.ConnectionFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.HttpURLConnection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@WebMvcTest(VIAF.class)
public class VIAFNonLiveTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        ConnectionFactory connectionFactory() throws Exception {
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
            return connectionFactory;
        }

        // we can't use MockBean b/c the PostConstruct hook in VIAF uses config
        // before we get a chance to put matchers on it in this test code.
        @Bean
        public Config config() {
            Config config = mock(Config.class);
            when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());
            return config;
        }
    }

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
        String json = "{\"q0\":{\"query\": \"shakespeare\",\"type\":\"/people/person\",\"type_strict\":\"should\"},\"q1\":{\"query\":\"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals(2, root.size());

        JsonNode result = root.get("q0").get("result");
        assertEquals(3, result.size());
        assertEquals("96994048", result.get(0).get("id").asText());
        assertEquals("Shakespeare, William, 1564-1616.", result.get(0).get("name").asText());
        assertEquals("/people/person", result.get(0).get("type").get(0).get("id").asText());
        assertEquals("Person", result.get(0).get("type").get(0).get("name").asText());
        assertEquals("0.3125", result.get(0).get("score").asText());
        assertFalse(result.get(0).get("match").asBoolean());

//            SearchResponse response2 = results.get("q1");
//            List<Result> result2 = response2.getResult();
//            assertEquals(result2.size(), 3);
//            assertEquals(result2.get(0).getId(), "24609378");
//            assertEquals(result2.get(0).getName(), "Wittgenstein, Ludwig, 1889-1951");
//            assertEquals(result2.get(0).getType().get(0).getId(), "/people/person");
//            assertEquals(result2.get(0).getType().get(0).getName(), "Person");
//            assertEquals(String.valueOf(result2.get(0).getScore()), "0.3548387096774194");
//            assertEquals(result2.get(0).isMatch(), false);
    }

}
