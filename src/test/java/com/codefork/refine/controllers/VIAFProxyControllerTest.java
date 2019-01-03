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
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class VIAFProxyControllerTest {

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
    public void testProxyMetaData() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/viafproxy/LC")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("LC (by way of VIAF)", root.get("name").asText());
        assertEquals("http://id.loc.gov/authorities/names/{{id}}", root.get("view").get("url").asText());
    }

    @Test
    public void testSearchProxyModeLC() throws Exception {
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
        assertEquals("Hauptmann, Gerhart, 1862-1946.", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("n80076391", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Pasternak, Boris Leonidovich, 1890-1960.", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("n79018438", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * VIAF gives a URL for the BNF source record ID. this test
     * checks that we use the ID parsed out of the "sid" XML element instead.
     */
    @Test
    public void testSearchProxyModeBNF() throws Exception {

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
        assertEquals("Hauptmann, Gerhart, 1862-1946.", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("12026662", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Pasternak, Boris Leonidovič, 1890-1960", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("11918737", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * Test case for XML missing an ID in ../mainHeadings/data/sources
     * but having an ID under ../VIAFCluster/sources.
     */
    @Test
    public void testSearchProxyModeBNFMissingID() throws Exception {
        String json = "{\"q0\":{\"query\": \"Jean-François Alexandre 1804 1874\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/BNF").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        // this entry in XML is missing an ID ../mainHeadings/data/sources
        JsonNode result1 = results.get(0);
        assertEquals("Alexandre, Jean-François 1804-1874", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("10341017", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Blanc, François 166.-1742", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("10343440", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Arago, Jacques, 1790-1855", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("12265696", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * Test case for URL in source ID mapping, which formatResult() should treat
     * as special case
     */
    @Test
    public void testSearchProxyModeDNB() throws Exception {
        String json = "{\"q0\":{\"query\": \"hegel\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/DNB").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Hegel, Georg Wilhelm Friedrich, 1770-1831.", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("118547739", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Koyré, Alexandre, 1892-1964", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("118777890", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Friedrich, Carl J. 1901-1984", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("118535870", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

}
