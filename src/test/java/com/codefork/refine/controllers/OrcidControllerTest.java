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
public class OrcidControllerTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @Autowired
    public MockConnectionFactoryHelper mockConnectionFactoryHelper;

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
    public void testSearch() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://pub.orcid.org/v2.1/search/?rows=3&q=stephen%20hawking",
                "/orcid_stephen_hawking.xml");
        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://pub.orcid.org/v2.1/0000-0002-4166-6322/record",
                "/0000-0002-4166-6322.xml");
        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://pub.orcid.org/v2.1/0000-0002-5081-5887/record",
                "/0000-0002-5081-5887.xml");
        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://pub.orcid.org/v2.1/0000-0002-9079-593X/record",
                "/0000-0002-9079-593X.xml");

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
        assertEquals("0.8666666666666667", result1.get("score").asText());
        assertFalse(result1.get("match").asBoolean());
    }

}
