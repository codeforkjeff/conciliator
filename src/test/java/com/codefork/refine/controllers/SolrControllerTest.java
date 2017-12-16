package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.datasource.SimulatedConnectionFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SolrControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return new SimulatedConnectionFactory();
        }
        // we can't use MockBean b/c the PostConstruct hook in VIAF uses config
        // before we get a chance to put matchers on it in this test code.
        @Bean
        public Config config() {
            Properties props = new Properties();
            props.setProperty("datasource.solr.nametype.id", "/book/book");
            props.setProperty("datasource.solr.nametype.name", "Book");
            props.setProperty("datasource.solr.url.query", "http://localhost:8983/solr/test-core/select?wt=xml&q={{QUERY}}&rows={{ROWS}}");
            props.setProperty("datasource.solr.url.document", "http://localhost:8983/solr/test-core/get?id={{id}}");
            props.setProperty("datasource.solr.field.id", "id");
            props.setProperty("datasource.solr.field.name", "title_display");

            Config config = new Config();
            config.merge(props);
            return config;
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    public void testSearchPersonalName() throws Exception {

        String json = "{\"q0\":{\"query\": \"The Complete Adventures of Sherlock Holmes\",\"type\":\"/book/book\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/solr").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("The complete Sherlock Holmes", result1.get("name").asText());
        assertEquals("119390", result1.get("id").asText());
        assertEquals("0.33383894", result1.get("score").asText());

        JsonNode result2 = results.get(1);
        assertEquals("The adventures of Sherlock Holmes", result2.get("name").asText());
        assertEquals("274753", result2.get("id").asText());
        assertEquals("0.26951128", result2.get("score").asText());

        JsonNode result3 = results.get(2);
        assertEquals("The adventures of Sherlock Holmes", result3.get("name").asText());
        assertEquals("25950", result3.get("id").asText());
        assertEquals("0.2694855", result3.get("score").asText());
    }

}
