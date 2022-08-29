package com.codefork.refine.controllers;

import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.datasource.MockConnectionFactoryHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public class OpenLibraryControllerTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @Autowired
    public MockConnectionFactoryHelper mockConnectionFactoryHelper;

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

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://openlibrary.org/search.json?q=through%20the%20arc%20of%20the%20rainforest",
                "/openlibrary_rainforest.json");

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
