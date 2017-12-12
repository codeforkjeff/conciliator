package com.codefork.refine.orcid;

import com.codefork.refine.Config;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@WebMvcTest(Orcid.class)
public class OrcidTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ConnectionFactory connectionFactory() {
            return new ConnectionFactory();
        }

        // we can't use MockBean b/c the PostConstruct hook in VIAF uses config
        // before we get a chance to put matchers on it in this test code.
        @Bean
        public Config config() {
            Properties props = new Properties();
            props.put("name", "ORCID");

            Config config = mock(Config.class);
            when(config.getDataSourceProperties("orcid")).thenReturn(props);
            return config;
        }

        @Bean
        public ThreadPoolFactory threadPoolFactory() {
            return new ThreadPoolFactory();
        }
    }

    @Autowired
    Orcid orcid;

    @Autowired
    MockMvc mvc;

    @Test
    public void testOrcidServiceMetaData() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/orcid")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("ORCID", root.get("name").asText());
    }

    @Test
    public void testSmartNamesServiceMetaData() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/orcid/smartnames")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("ORCID - Smart Names Mode", root.get("name").asText());
    }

    @Test
    public void testLiveSearch() throws Exception {

        String json = "{\"q0\":{\"query\": \"stephen hawking\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/orcid").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Stephen Hawking", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("0000-0002-9079-593X", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());
    }

    // https://github.com/codeforkjeff/conciliator/issues/8
    @Test
    public void testLiveSearchSmartNames() throws Exception {

        String json = "{\"q0\":{\"query\": \"Igor Ozerov\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/orcid/smartnames").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(1, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Igor OZEROV", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("0000-0001-5839-7854", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());
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
