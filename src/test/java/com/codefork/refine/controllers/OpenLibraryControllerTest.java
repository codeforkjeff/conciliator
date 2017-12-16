package com.codefork.refine.controllers;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OpenLibraryControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return new SimulatedConnectionFactory();
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    public void testServiceMetadata() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/openlibrary")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("OpenLibrary", root.get("name").asText());
        assertEquals("https://openlibrary.org{{id}}", root.get("view").get("url").asText());
    }

    @Test
    public void testSearch() throws Exception {

        String json = "{\"q0\":{\"query\": \"through the arc of the rainforest\",\"type\":\"/book/book\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/openlibrary").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals(1, root.size());

        assertEquals(1, root.get("q0").get("result").size());
        JsonNode result = root.get("q0").get("result").get(0);
        assertEquals("/works/OL9467604W", result.get("id").asText());
        assertEquals("Through the Arc of the Rainforest", result.get("name").asText());
        assertEquals("/book/book", result.get("type").get(0).get("id").asText());
        assertFalse(result.get("match").asBoolean());
    }
}
