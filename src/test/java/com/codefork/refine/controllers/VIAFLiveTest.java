package com.codefork.refine.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public class VIAFLiveTest {

    @Autowired
    MockMvc mvc;

    /**
     * Simple test for parsing live VIAF XML
     */
    @Test
    public void testLiveSearch() throws Exception {

        String json = "{\"q0\":{\"query\": \"shakespeare\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString(Charset.defaultCharset());

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        // first result for shakespeare shouldn't ever change;
        // it makes for a fairly stable super-basic live test

        JsonNode result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("96994048", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());
    }

    @Test
    public void testLiveSearchEncoding() throws Exception {

        String json = "{\"q0\":{\"query\": \"Nabokov, Vladimir Vladimirovič\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf/NSK").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString(Charset.defaultCharset());

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Nabokov, Vladimir Vladimirovič", result1.get("name").asText());
        assertEquals("/people/person", result1.get("type").get(0).get("id").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("27069388", result1.get("id").asText());
    }

}
