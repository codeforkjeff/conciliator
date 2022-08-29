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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SolrControllerTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @Autowired
    public MockConnectionFactoryHelper mockConnectionFactoryHelper;

    @Autowired
    MockMvc mvc;

    @Test
    public void testSearchPersonalName() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "http://localhost:8983/solr/test-core/select?wt=xml&q=The%20Complete%20Adventures%20of%20Sherlock%20Holmes&rows=3",
                "/solr_results.xml");

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
